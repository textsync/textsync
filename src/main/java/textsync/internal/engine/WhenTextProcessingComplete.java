package textsync.internal.engine;

public interface WhenTextProcessingComplete {

    public void onSuccess(String text);

    public void onFailure(Throwable t);
}