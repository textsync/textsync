package textsync;

import io.nextweb.Session;
import io.nextweb.common.LoginResult;
import io.nextweb.common.User;
import io.nextweb.jre.Nextweb;
import io.nextweb.operations.exceptions.ChallengedResult;
import io.nextweb.operations.exceptions.LoginFailuresListener;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

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
import textsync.internal.engine.Operation;
import textsync.internal.engine.ProcessFilesProcess;
import textsync.internal.engine.ProcessFilesProcess.ProcessFilesParameters;
import textsync.internal.engine.ProcessFilesProcess.WhenFilesProcessed;
import textsync.internal.ui.MainFrame;
import de.mxro.fn.Closure;
import de.mxro.javafileutils.monitor.FileCache;

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

		final String file = args[7];
		final String username = args[1];
		final String password = args[3];
		
		final CountDownLatch latch = new CountDownLatch(2);

		latch.countDown();

		final List<Throwable> exceptions = Collections
				.synchronizedList(new LinkedList<Throwable>());


		final Session session = Nextweb.createSession();

		System.out.println("Logging in user ...");
		
		LoginResult loginRq = session.login(username, password);
		
		loginRq.catchLoginFailures(new DefaultLoginHandler(latch, exceptions));
		
		loginRq.get(new Closure<User>() {

			@Override
			public void apply(User o) {
				final AppjangleDataService dataService = new AppjangleDataService(
						session, o);

				final FileCache fileCache = new FileCache();

				ProcessFilesParameters params = new ProcessFilesProcess.ProcessFilesParameters() {

					@Override
					public File inputFile() {

						return new File(file);
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
								
								session.close().get();
								
								System.out
								.println("Synchronization completed successfully.");
						latch.countDown();
								
								
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
	
	private static final class DefaultLoginHandler implements LoginFailuresListener {
		private final CountDownLatch latch;
	
		private final List<Throwable> exceptions;

		private DefaultLoginHandler(CountDownLatch latch, 
				List<Throwable> exceptions) {
			this.latch = latch;

			this.exceptions = exceptions;
		}

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
		public void onChallenged(ChallengedResult arg0) {
			exceptions.add(new Exception(
					"Unexpected challenge received."));
			latch.countDown();
		}

		@Override
		public void onUserAlreadyRegistered() {
			exceptions.add(new Exception(
					"Unexpected user already registered received."));
			latch.countDown();
		}

	
	}
}
