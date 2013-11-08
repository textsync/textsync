/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package textsync.internal.engine;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.mxro.javafileutils.Collect;
import de.mxro.javafileutils.monitor.FileCache;
import one.async.joiner.CallbackLatch;
import one.core.dsl.callbacks.WhenShutdown;
import one.utils.jre.OneUtilsJre;
import textsync.internal.DataService;
import textsync.internal.DataServiceFactory;
import textsync.internal.LogService;
import textsync.internal.engine.ProcessFilesProcess.WhenFilesProcessed;

/**
 * 
 * @author mroh004
 */
public class SyncManager {

	private static final boolean ENABLE_LOG = false;

	FileCache fileCache;
	DataServiceFactory dataServiceFactory;
	LogService logService;
	private volatile boolean backgroundSyncActive = false;
	private volatile boolean fastBackgroundSyncActive = false;
	private volatile boolean syncActive = false;

	Timer timer;
	FileProvider fileProvider;
	SyncEvents syncEventListener;
	private TimerTask syncTask;
	private TimerTask fileMonitorTask = null;
	private Set<String> filesProcessing;

	private Vector<SyncOperation> scheduledOperations;

	public static interface SyncOperation {
		public List<File> files();

		public SyncEvents listener();

		public Set<Operation> skipOperations();

	}

	public static interface FileProvider {

		public List<File> getFiles();
	}

	public static interface SyncEvents {

		public void synchronizationCompleted();

		public void initSynchronization(int maxValue);

		public void reportStatus(String status);

		public void updateProgressBar(int newValue);

		public void onFailure(Throwable t);
	}

	public void startAutomaticUploadOfChangedFiles() {
		logService.note("Starting file monitoring task ...");
		if (fileCache == null) {
			logService
					.note("Performing initial synchronization to initialize cache ...");

			fileCache = new FileCache();
			@SuppressWarnings("serial")
			Set<Operation> skippedOperations = new HashSet<Operation>() {
				{
					add(Operation.DOWNLOAD);
				}

			};
			scheduleSync(fileProvider.getFiles(), new SyncEvents() {

				public void synchronizationCompleted() {

				}

				public void initSynchronization(int maxValue) {

				}

				public void updateProgressBar(int newValue) {

				}

				public void onFailure(Throwable t) {

				}

				public void reportStatus(String status) {
					syncEventListener.reportStatus(status);
				}
			}, skippedOperations);
		}

		if (fileMonitorTask != null) {
			return;
		}

		fileMonitorTask = new TimerTask() {

			@Override
			public void run() {

				if (fastBackgroundSyncActive) {
					return;
				}

				fastBackgroundSyncActive = true;

				final Set<File> modifiedFiles = new HashSet<File>();
				for (File file : Collect
						.getFilesRecursively(fileProvider.getFiles())) {

					if (fileCache.hasCache(file) && fileCache.isModified(file)) {
						// System.out.println("File changed: " + file);
						modifiedFiles.add(file);
					}

				}

				if (modifiedFiles.size() > 0) {
					logService.note("Syncing modified files: " + modifiedFiles);
					@SuppressWarnings("serial")
					Set<Operation> skippedOperations = new HashSet<Operation>() {
						{
							add(Operation.DOWNLOAD);
						}
					};
					doPrioritySync(new ArrayList<File>(modifiedFiles),
							skippedOperations, new SyncEvents() {

								public void synchronizationCompleted() {
									fastBackgroundSyncActive = false;
									logService
											.note("Completed syncing modified files: "
													+ modifiedFiles);
								}

								public void initSynchronization(int maxValue) {
								}

								public void updateProgressBar(int newValue) {
								}

								public void onFailure(Throwable t) {
									fastBackgroundSyncActive = false;
									syncEventListener.onFailure(t);
								}

								public void reportStatus(String status) {
								}
							});
				} else {
					fastBackgroundSyncActive = false;
				}
			}
		};

		timer.scheduleAtFixedRate(fileMonitorTask, 500, 1000);

	}

	public void stopAutomaticUploadOfChangedFiles() {
		if (fileMonitorTask == null) {
			return;
		}

		fileMonitorTask.cancel();

		fileMonitorTask = null;

	}

	public void startPeriodicFullSynchronization() {
		if (syncTask != null) {
			return;
		}
		syncTask = new TimerTask() {

			@Override
			public void run() {
				if (backgroundSyncActive) {
					return;
				}

				doFullSync(fileProvider.getFiles(), syncEventListener);
			}
		};

		timer.scheduleAtFixedRate(syncTask, 10, 1000 * 60);
	}

	public void stopPeriodicFullSynchronization() {
		if (syncTask == null) {
			return;
		}

		syncTask.cancel();

		syncTask = null;
	}

	public void doFullSynchronization() {
		fileCache = new FileCache();

		doFullSync(fileProvider.getFiles(), syncEventListener);
	}

	/**
	 * Attempt to trigger a sync of the specified files as fast as possible.
	 * 
	 * @param files
	 * @param listener
	 */
	public synchronized void doPrioritySync(final List<File> files,
			final Set<Operation> skippedOperations, final SyncEvents listener) {
		scheduleSync(files, listener, skippedOperations);
	}

	public synchronized void doPrioritySyncDelayed(final List<File> files,
			final Set<Operation> skippedOperations, final int delayInMs,
			final SyncEvents listener) {
		new Thread(new Runnable() {

			public void run() {
				try {
					Thread.sleep(delayInMs);
				} catch (InterruptedException ex) {
					listener.onFailure(ex);
					throw new RuntimeException(ex);
				}
				doPrioritySync(files, skippedOperations, listener);

			}
		}).start();
	}

	private synchronized void scheduleSync(final List<File> files,
			final SyncEvents listener, final Set<Operation> skipOperations) {

		synchronized (scheduledOperations) {

			if (ENABLE_LOG) {
				System.out.println("Schedule sync for: " + files);
			}

			scheduledOperations.add(new SyncOperation() {

				public List<File> files() {
					return files;
				}

				public SyncEvents listener() {
					return listener;
				}

				public Set<Operation> skipOperations() {
					return skipOperations;
				}

			});

			performSyncs();
		}

	}

	private void performSyncs() {
		synchronized (scheduledOperations) {
			if (syncActive) {

				return;
			}

			if (scheduledOperations.size() == 0) {
				return;
			}

			final SyncOperation toPerform = scheduledOperations.get(0);
			scheduledOperations.remove(0);

			syncActive = true;
			doSync(toPerform.files(), toPerform.skipOperations(),
					new SyncEvents() {

						@Override
						public void updateProgressBar(int newValue) {
							toPerform.listener().updateProgressBar(newValue);
						}

						@Override
						public void synchronizationCompleted() {

							toPerform.listener().synchronizationCompleted();
							syncActive = false;
							performSyncs();
						}

						@Override
						public void reportStatus(String status) {
							toPerform.listener().reportStatus(status);
						}

						@Override
						public void onFailure(Throwable t) {
							toPerform.listener().onFailure(t);
							syncActive = false;
							performSyncs();
						}

						@Override
						public void initSynchronization(int maxValue) {
							toPerform.listener().initSynchronization(maxValue);
						}

					});

		}
	}

	private synchronized void doSync(final List<File> files,
			final Set<Operation> skipOperations, final SyncEvents listener) {

		listener.initSynchronization((files.size() * 2) + 3);

		final AtomicBoolean active = new AtomicBoolean(true);

		final CallbackLatch latch = new CallbackLatch(files.size()) {

			@Override
			public void onCompleted() {
				// listener.reportStatus("  Winding down data service. " + new
				// Date());

				active.set(false);
				listener.reportStatus("Finished Synchronization: " + new Date());

				listener.synchronizationCompleted();
			}

			@Override
			public void onFailed(Throwable thrwbl) {
				logService.note("  Exception occured: " + thrwbl.getMessage());
				listener.onFailure(thrwbl);
				throw new RuntimeException(thrwbl);
			}
		};

		Thread syncThread = new Thread(new Runnable() {

			public void run() {
				for (final File item : files) {

					try {

						listener.updateProgressBar(+1);

						if (filesProcessing.contains(item.getAbsolutePath())) {
							listener.reportStatus("  Defer processing for: "
									+ item);
							Set<Operation> skippedOperations = new HashSet<Operation>();
							doPrioritySyncDelayed(Arrays.asList(item),
									skippedOperations, 1000, new SyncEvents() {

										public void synchronizationCompleted() {

											listener.reportStatus("  Processed deferred item: "
													+ item);
											latch.registerSuccess();
										}

										public void initSynchronization(
												int maxValue) {
										}

										public void reportStatus(String status) {
											listener.reportStatus(status);
										}

										public void updateProgressBar(
												int newValue) {
										}

										public void onFailure(Throwable t) {
											logService
													.note("  Unexpected exception while processing deferred item: "
															+ item
															+ " "
															+ t.getMessage());
											latch.registerFail(t);
											throw new RuntimeException(t);
										}
									});

							return;
						}

						filesProcessing.add(item.getAbsolutePath());

						dataServiceFactory
								.createDataService(new DataServiceFactory.WhenDataServiceCreated() {

									public void onFailure(Throwable t) {
										logService.note(t.getMessage());
										throw new RuntimeException(t);
									}

									public void thenDo(
											final DataService dataService) {
										try {

											ProcessFilesProcess.ProcessFilesParameters p = new ProcessFilesProcess.ProcessFilesParameters() {

												public File inputFile() {
													return item;
												}

												public DataService dataService() {
													return dataService;
												}

												public LogService logService() {
													return logService;
												}

												public FileCache cache() {
													return fileCache;
												}

												public Set<Operation> skippedOperations() {
													return skipOperations;
												}

												public WhenFilesProcessed callback() {
													return new ProcessFilesProcess.WhenFilesProcessed() {

														public void onSuccess() {
															listener.updateProgressBar(+1);
															filesProcessing
																	.remove(item
																			.getAbsolutePath());
															dataService
																	.shutdown(new WhenShutdown() {

																		@Override
																		public void thenDo() {
																			latch.registerSuccess();
																		}

																		@Override
																		public void onFailure(
																				Throwable t) {
																			latch.registerFail(t);
																		}
																	});

														}

														public void onFailure(
																final Throwable t) {
															filesProcessing
																	.remove(item
																			.getAbsolutePath());
															dataService
																	.shutdown(new WhenShutdown() {

																		@Override
																		public void thenDo() {
																			latch.registerFail(t);
																		}

																		@Override
																		public void onFailure(
																				Throwable t) {
																			latch.registerFail(t);
																		}
																	});

														}
													};
												}
											};

											ProcessFilesProcess.processFile(p);
										} catch (Exception t) {
											latch.registerFail(t);
										}
									}
								});

					} catch (Exception e) {
						logService.note(e.getMessage());
						latch.registerFail(e);
					}

				}
			}
		});

		Thread monitorSyncThread = new Thread(new Runnable() {

			public void run() {
				try {
					Thread.sleep(240 * 1000); // wait for 2 minutes
					if (active.get()) {

						logService.note("Error: Synchronization timeout "
								+ new Date());
						listener.onFailure(new Exception(
								"Error: Synchronization timeout " + new Date()));
					}

				} catch (InterruptedException ex) {
					logService.note("  Unexpected exception: " + ex);
				}
			}
		});

		syncThread.start();
		monitorSyncThread.start();
	}

	private synchronized void doFullSync(final List<File> files,
			final SyncEvents listener) {

		backgroundSyncActive = true;

		logService.note("Start Full Synchronization: " + new Date());

		Set<Operation> skippedOperations = new HashSet<Operation>();
		scheduleSync(files, new SyncEvents() {

			public void synchronizationCompleted() {
				backgroundSyncActive = false;
				listener.synchronizationCompleted();
			}

			public void initSynchronization(int maxValue) {
				listener.initSynchronization(maxValue);
			}

			public void updateProgressBar(int newValue) {
				listener.updateProgressBar(newValue);
			}

			public void onFailure(Throwable t) {
				backgroundSyncActive = false;
				listener.onFailure(t);
			}

			public void reportStatus(String status) {
				listener.reportStatus(status);
			}
		}, skippedOperations);

	}

	public SyncManager(DataServiceFactory dataServiceFactory,
			LogService logService, FileProvider fileProvider,
			SyncEvents syncEventListener) {
		this.dataServiceFactory = dataServiceFactory;
		this.logService = logService;
		this.fileProvider = fileProvider;
		this.syncEventListener = syncEventListener;
		this.fileCache = null;// new FileCache();
		this.filesProcessing = Collections
				.synchronizedSet(new HashSet<String>());
		this.timer = new Timer();
		this.scheduledOperations = new Vector<SyncManager.SyncOperation>();
	}
}
