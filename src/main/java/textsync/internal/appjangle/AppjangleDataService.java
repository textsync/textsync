/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package textsync.internal.appjangle;

import one.common.extend.OneExtend;
import one.core.domain.OneClient;
import one.core.dsl.CoreDsl;
import one.core.dsl.callbacks.WhenCommitted;
import one.core.dsl.callbacks.WhenLoaded;
import one.core.dsl.callbacks.WhenResponseFromServerReceived;
import one.core.dsl.callbacks.WhenShutdown;
import one.core.dsl.callbacks.WhenVersionsCleared;
import one.core.dsl.callbacks.results.WithCommittedResult;
import one.core.dsl.callbacks.results.WithLoadResult;
import one.core.dsl.callbacks.results.WithOperationResult;
import one.core.dsl.callbacks.results.WithUserRegisteredResult;
import one.core.dsl.callbacks.results.WithVersionsClearedResult;
import one.core.nodes.OneNode;
import one.core.nodes.OneValue;
import textsync.internal.DataService;

/**
 *
 * @author Max
 */
public class AppjangleDataService implements DataService {

    private final OneClient session;
    private final WithUserRegisteredResult loginDetails;

    public AppjangleDataService(OneClient client, WithUserRegisteredResult loginDetails) {
        this.session = client;
        this.loginDetails = loginDetails;
    }

    public void uploadChanges(final String newValue, String nodeUri, final WhenChangesUploaded callback) {

        final CoreDsl dsl = session.one();
         
        dsl.reload(dsl.reference(nodeUri)).withSecret(loginDetails.userNodeSecret()).in(session).and(new WhenLoaded() {

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

                String oldValue = (String) valueNode.getValue();

                if (oldValue.equals(newValue)) {
                    callback.thenDo(false);
                    return;
                }

                OneValue<String> newValueNode = dsl.newNode(newValue).at(valueNode.getId());

                dsl.replaceSafe(valueNode).with(newValueNode).in(session).and(new WhenResponseFromServerReceived<OneValue<String>>() {

                    @Override
                    public void thenDo(WithOperationResult<OneValue<String>> wor) {
                        dsl.clearVersions(wor.node()).andKeepOnServer(3).in(session).and(new WhenVersionsCleared() {

                                    @Override
                                    public void thenDo(WithVersionsClearedResult wvcr) {
                                        callback.thenDo(true);
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
    public void createNewNode(final String value, final String title, final String extension, final boolean isPublic, final WhenNewNodeCreated callback) {

        assertAjFileSyncDataNode(new WhenSyncDataNodeAsserted() {

            public void thenDo(OneNode syncDataNode) {
                final CoreDsl dsl = session.one();
               
                dsl.appendSafe(value).to(syncDataNode).atClosestAddress("./" + title).withSecret(loginDetails.userNodeSecret()).in(session).and(new WhenResponseFromServerReceived<OneValue<String>>() {

                    @Override
                    public void thenDo(final WithOperationResult<OneValue<String>> wor) {

                        dsl.append(dsl.reference("https://u1.linnk.it/6wbnoq/Types/aTextValue")).to(wor.node()).in(session);

                        dsl.append(dsl.reference("https://u1.linnk.it/6wbnoq/Types/aTemplate")).to(wor.node()).in(session);

                        if (extension.equals("md")) {
                            dsl.append(dsl.reference("https://u1.linnk.it/6wbnoq/Types/isMarkdown")).to(wor.node()).in(session);
                            dsl.append(dsl.reference("https://admin1.linnk.it/types/v01/isHtmlValue")).to(wor.node()).in(session);
                        }

                        if (extension.equals("html")) {
                            dsl.append(dsl.reference("https://admin1.linnk.it/types/v01/isHtmlValue")).to(wor.node()).in(session);
                        }

                        if (isPublic) {
                            dsl.append(dsl.newNode().asPublicReadToken()).to(wor.node()).in(session);
                        }
                        
                        dsl.commit(session).and(new WhenCommitted() {

                            @Override
                            public void thenDo(WithCommittedResult wcr) {
                               
                                callback.thenDo(wor.node());
                            }
                        });
                        
                        
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        callback.onFailure(t);
                    }
                });


            }

            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        });

    }

    public void downloadChanges(final String localValue, String nodeUri, final WhenChangesDownloaded callback) {
       final CoreDsl dsl = session.one();

        dsl.reload(nodeUri).withSecret(loginDetails.userNodeSecret()).in(session).and(new WhenLoaded() {

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
        session.one().shutdown(session).and(callback);
    }

    private interface WhenSyncDataNodeAsserted {

        public void thenDo(OneNode syncDataNode);

        public void onFailure(Throwable t);
    }

    private void assertAjFileSyncDataNode(final WhenSyncDataNodeAsserted callback) {

        CoreDsl dsl = session.one();

       
        
        dsl.load(loginDetails.userNodeUri()).withSecret(loginDetails.userNodeSecret()).in(session).and(new WhenLoaded() {

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
