package services.vortex.toastr.utils;

import services.vortex.toastr.ToastrPlugin;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        ToastrPlugin.getInstance().getLogger().warn(r.toString() + " has been rejected from " + executor.toString());
    }

}
