package aj.apps.microsync.tests;

import one.client.jre.OneJre;
import one.core.domain.OneClient;
import one.core.dsl.CoreDsl;
import one.core.dsl.callbacks.WhenCommitted;
import one.core.dsl.callbacks.WhenLoaded;
import one.core.dsl.callbacks.WhenShutdown;
import one.core.dsl.callbacks.results.WithCommittedResult;
import one.core.dsl.callbacks.results.WithLoadResult;

public class MakeNodesPublic {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final CoreDsl dsl = OneJre.init();
		
		final OneClient client = dsl.createClient();
		
		dsl.load("").withSecret("").in(client).and(new WhenLoaded() {
			
			@Override
			public void thenDo(WithLoadResult<Object> r) {
				dsl.append(dsl.reference("https://u1.linnk.it/6wbnoq/Types/aTextValue")).to(r.loadedNode()).in(client);

		        dsl.append(dsl.reference("https://u1.linnk.it/6wbnoq/Types/aTemplate")).to(r.loadedNode()).in(client);
				
				dsl.append(dsl.reference("https://admin1.linnk.it/types/v01/isHtmlValue")).to(r.loadedNode()).in(client);
				
				dsl.append(dsl.newNode().asPublicReadToken()).to(r.loadedNode()).in(client);
				
				dsl.commit(client).and(new WhenCommitted() {
					
					@Override
					public void thenDo(WithCommittedResult r) {
						System.out.println("All done.");
						dsl.shutdown(client).and(WhenShutdown.DO_NOTHING);
					}
				});
			}
		});
		
		

	}

}
