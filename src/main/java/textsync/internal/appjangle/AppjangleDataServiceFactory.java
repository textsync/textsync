/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package textsync.internal.appjangle;

import io.nextweb.Session;
import io.nextweb.common.User;
import one.core.domain.OneClient;
import one.core.dsl.CoreDsl;
import one.core.dsl.callbacks.WhenLoaded;
import one.core.dsl.callbacks.results.WithLoadResult;
import one.core.dsl.callbacks.results.WithUserRegisteredResult;
import textsync.internal.DataServiceFactory;

/**
 *
 * @author mroh004
 */
public class AppjangleDataServiceFactory implements DataServiceFactory {

    Session session;
    User user;
    
    public void createDataService(final WhenDataServiceCreated callback) {
        final OneClient client = dsl.createClient();
        
        dsl.load(wurr.userNodeUri()).withSecret(wurr.userNodeSecret()).in(client).and(new WhenLoaded() {

            @Override
            public void thenDo(WithLoadResult<Object> wlr) {
                callback.thenDo(new AppjangleDataService(client, wurr));
            }
        });
        
       
    }

    public AppjangleDataServiceFactory(Session session, User user) {
        this.session = session;
        this.user = user;
    }
    
    
    
}
