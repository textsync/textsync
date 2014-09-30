/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package textsync.internal.appjangle;

import io.nextweb.Node;
import io.nextweb.Session;
import io.nextweb.common.User;
import one.common.extend.OneExtend;
import one.core.dsl.CoreDsl;
import one.core.dsl.callbacks.WhenCommitted;
import one.core.dsl.callbacks.WhenLoaded;
import one.core.dsl.callbacks.WhenResponseFromServerReceived;
import one.core.dsl.callbacks.WhenShutdown;
import one.core.dsl.callbacks.WhenVersionsCleared;
import one.core.dsl.callbacks.results.WithCommittedResult;
import one.core.dsl.callbacks.results.WithLoadResult;
import one.core.dsl.callbacks.results.WithOperationResult;
import one.core.dsl.callbacks.results.WithVersionsClearedResult;
import one.core.nodes.OneNode;
import one.core.nodes.OneValue;
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

       throw new RuntimeException("Not supported.")

    }

    public void downloadChanges(final String localValue, String nodeUri, final WhenChangesDownloaded callback) {
       final CoreDsl dsl = session.one();

        dsl.reload(nodeUri).withSecret(user.userNodeSecret()).in(session).and(new WhenLoaded() {

            @Override
            public void thenDo(WithLoadResult<Object> wlr) {
                Object resolvedNode = dsl.dereference(wlr.loadedNode()).in(session);

                if (!(resolvedNode instanceof OneValue<?>)) {
                    callback.onFailure(new Exception("Node was not of type OneValue: " + wlr.loadedNode()));
                    return;
                }

                OneValue<?> valueNode = (OneValue<?>) resolvedNode;

                if (!(valueNode.getValue() instanceof String)) {
                    callback.onFailure(new Exception("Value nodes value was not of type String: " + valueNode));
                    return;
                }

                String remoteValue = (String) valueNode.getValue();

                if (remoteValue.equals(localValue)) {
                    callback.onUnchanged();
                    return;
                }
                
                callback.onChanged(remoteValue);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
            
            
        });
        
        
    }

    public void shutdown(WhenShutdown callback) {
        session.close().get();
        callback.thenDo();
    }

    private interface WhenSyncDataNodeAsserted {

        public void thenDo(OneNode syncDataNode);

        public void onFailure(Throwable t);
    }

    private void assertAjFileSyncDataNode(final WhenSyncDataNodeAsserted callback) {

        CoreDsl dsl = session.one();

       
        
        dsl.load(user.userNodeUri()).withSecret(user.userNodeSecret()).in(session).and(new WhenLoaded() {

            @Override
            public void thenDo(WithLoadResult<Object> wlr) {

                assertChild(wlr.loadedNode(), "apps", new WhenLoaded() {

                    @Override
                    public void thenDo(WithLoadResult<Object> wlr) {

                        assertChild(wlr.loadedNode(), "textsync", new WhenLoaded() {

                            @Override
                            public void thenDo(WithLoadResult<Object> wlr) {

                                assertChild(wlr.loadedNode(), "upload", new WhenLoaded() {

                                    @Override
                                    public void thenDo(WithLoadResult<Object> wlr) {
                                        callback.thenDo(wlr.loadedNode());
                                    }

                                    @Override
                                    public void onFailure(Throwable t) {
                                        callback.onFailure(t);
                                    }
                                });

                            }

                            @Override
                            public void onFailure(Throwable t) {
                                callback.onFailure(t);
                            }
                        });

                    }

                    @Override
                    public void onFailure(Throwable t) {
                        callback.onFailure(t);
                    }
                });

            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        });



    }

    private void assertChild(final OneNode forNode, final String childPath, final WhenLoaded callback) {

        //final CoreDsl dsl = client.one();

        OneExtend.assertChild(session, forNode, childPath, callback);


    }
}
