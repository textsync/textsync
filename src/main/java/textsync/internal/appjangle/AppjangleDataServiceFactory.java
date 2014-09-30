/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package textsync.internal.appjangle;

import io.nextweb.Session;
import io.nextweb.common.User;
import textsync.internal.DataServiceFactory;

/**
 *
 * @author mroh004
 */
public class AppjangleDataServiceFactory implements DataServiceFactory {

    Session session;
    User user;
    
    public void createDataService(final WhenDataServiceCreated callback) {
    	callback.thenDo(new AppjangleDataService(session, user));
    	
        
       
    }

    public AppjangleDataServiceFactory(Session session, User user) {
        this.session = session;
        this.user = user;
    }
    
    
    
}
