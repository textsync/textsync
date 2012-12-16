package textsync;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import one.client.jre.OneJre;
import one.core.domain.OneClient;
import one.core.dsl.CoreDsl;
import one.core.dsl.callbacks.WhenShutdown;
import one.core.dsl.callbacks.WhenUserLoggedIn;
import one.core.dsl.callbacks.results.WithChallengedContext;
import one.core.dsl.callbacks.results.WithUserRegisteredResult;
import one.core.dsl.grammars.LoginWithUserDetailsParameters;
import textsync.internal.DataService;
import textsync.internal.LogService;
import textsync.internal.appjangle.AppjangleDataService;
import textsync.internal.engine.FileCache;
import textsync.internal.engine.Operation;
import textsync.internal.engine.ProcessFilesProcess;
import textsync.internal.engine.ProcessFilesProcess.ProcessFilesParameters;
import textsync.internal.engine.ProcessFilesProcess.WhenFilesProcessed;
import textsync.internal.ui.MainFrame;

/**
 * TextSync main class
 * 
 */
public class TextSync {
	public static void main(final String[] args) {
		if (args.length == 0) {
			MainFrame.main(args);
			return;
		}

		assert args[0].equals("-user");
		assert args[2].equals("-password");
		assert args[4].equals("-op");
		assert args[6].equals("-root");

		final CountDownLatch latch = new CountDownLatch(2);

		latch.countDown();

		final List<Throwable> exceptions = Collections
				.synchronizedList(new LinkedList<Throwable>());

		final CoreDsl dsl = OneJre.init();

		final OneClient createClient = dsl.createClient();

		System.out.println("Logging in user ...");
		dsl.loginUser(new LoginWithUserDetailsParameters() {

			@Override
			public String getPassword() {
				return args[3];
			}

			@Override
			public String getEmail() {
				return args[1];
			}

			@Override
			public OneClient getClient() {

				return createClient;
			}

			@Override
			public String getApplicationNodeUri() {
				return "https://u1.linnk.it/0fs7dr/Apps1/appjangle";
			}

			@Override
			public String getApplicationNodeSecret() {
				return "";
			}

			@Override
			public WhenUserLoggedIn getCallback() {
				return new WhenUserLoggedIn() {

					@Override
					public void onNotRegisteredForApplication() {
						exceptions.add(new Exception(
								"User not registered for Appjangle."));
						latch.countDown();
					}

					@Override
					public void onInvalidDetails() {
						exceptions.add(new Exception("Invalid login details."));
						latch.countDown();
					}

					@Override
					public void onFailure(Throwable arg0) {
						exceptions.add(arg0);
						latch.countDown();
					}

					@Override
					public void onChallenge(WithChallengedContext arg0) {
						exceptions.add(new Exception(
								"Unexpected challenge received."));
						latch.countDown();
					}
					
					@Override
					public void thenDo(WithUserRegisteredResult loginDetails) {
						System.out.println("Processing ["+args[7]+"] ...");
						
						final AppjangleDataService dataService = new AppjangleDataService(
								createClient, loginDetails);

						final FileCache fileCache = new FileCache();

						ProcessFilesParameters params = new ProcessFilesProcess.ProcessFilesParameters() {

							@Override
							public File inputFile() {
								return new File(args[7]);
							}

							@Override
							public DataService dataService() {

								return dataService;
							}

							@Override
							public LogService logService() {

								return new LogService() {

									@Override
									public void note(String text) {
										System.out.println(text);
									}
								};
							}

							@Override
							public FileCache cache() {

								return fileCache;
							}

							@Override
							public Set<Operation> skippedOperations() {

								HashSet<Operation> ops = new HashSet<Operation>();

								if (args[5].equals("synchronize")) {
									// do all
								} else if (args[5].equals("download")) {
									ops.add(Operation.UPLOAD);
									ops.add(Operation.UPLOADNEW);
									ops.add(Operation.UPLOADPUBLIC);
								} else if (args[5].equals("upload")) {
									ops.add(Operation.DOWNLOAD);
								}

								return ops;
							}

							@Override
							public WhenFilesProcessed callback() {

								return new WhenFilesProcessed() {

									@Override
									public void onSuccess() {
										
										dsl.shutdown(createClient).and(new WhenShutdown() {
											
											@Override
											public void thenDo() {
												System.out
												.println("Synchronization completed successfully.");
										latch.countDown();
											}

											@Override
											public void onFailure(Throwable arg0) {
												exceptions.add(arg0);
												latch.countDown();
											}
											
											
										});
										
									}

									@Override
									public void onFailure(Throwable t) {
										exceptions.add(t);
										latch.countDown();
									}
								};
							}

						};
						
						try {
							ProcessFilesProcess.processFile(params);
						} catch (Exception e) {
							exceptions.add(e);
							latch.countDown();
						}
					}

					
				};
				
			}
		});

		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		if (exceptions.size() > 0) {
			throw new RuntimeException(exceptions.get(0));
		}
		
	}
}
