/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package textsync.internal.appjangle;

import io.nextweb.Node;
import io.nextweb.Query;
import io.nextweb.Session;
import io.nextweb.common.User;
import io.nextweb.promise.exceptions.ExceptionListener;
import io.nextweb.promise.exceptions.ExceptionResult;
import one.core.dsl.callbacks.WhenShutdown;
import textsync.internal.DataService;

/**
 *
 * @author Max
 */
public class AppjangleDataService implements DataService {

    private final Session session;
    private final User user;

    public AppjangleDataService(Session session, User user) {
        this.session = session;
        this.user = user;
    }

    public void uploadChanges(final String newValue, String nodeUri, final WhenChangesUploaded callback) {
    	Node resolvedNode = session.link(nodeUri, user.userNode().secret()).reload().get();

        String oldValue = resolvedNode.value(String.class);

        if (oldValue.equals(newValue)) {
            callback.thenDo(false);
            return;
        }

        resolvedNode.setValue(newValue).get();
        
        resolvedNode.clearVersions(3).get();
        
        callback.thenDo(true);
       

    }

    @Override
    public void createNewNode(final String value, final String title, final String extension, final boolean isPublic, final WhenNewNodeCreated callback) {

       throw new RuntimeException("Not yet supported.");

    }

    public void downloadChanges(final String localValue, String nodeUri, final WhenChangesDownloaded callback) {
      
    	Query query = session.link(nodeUri, user.userNode().secret()).reload();
    	
    	query.catchExceptions(new ExceptionListener() {
			
			@Override
			public void onFailure(ExceptionResult arg0) {
				callback.onFailure(arg0.exception());
			}
		});
    	
    	 String remoteValue = resolvedNode.value(String.class);

         if (remoteValue.equals(localValue)) {
             callback.onUnchanged();
             return;
         }
         
         callback.onChanged(remoteValue);
         
       
        
    }

    public void shutdown(WhenShutdown callback) {
        //session.close().get();
        callback.thenDo();
    }

   
   
}
