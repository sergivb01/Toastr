package dev.sergivos.toastr.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import dev.sergivos.toastr.ToastrPlugin;
import dev.sergivos.toastr.utils.CC;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitListener {
    private static final ToastrPlugin instance = ToastrPlugin.getInstance();
    private static final int MAX_LOGIN_SEC = 12;
    private final AtomicInteger loginsSec = new AtomicInteger(0);

    public RateLimitListener() {
        instance.getProxy().getScheduler()
                .buildTask(instance, () -> loginsSec.set(0)).repeat(1, TimeUnit.SECONDS)
                .schedule();
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPreLogin(PreLoginEvent event) {
        if(loginsSec.get() >= MAX_LOGIN_SEC) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(CC.translate("&cRate limit reached\n please try again in few seconds")));
        }
    }

    @Subscribe
    public void onPostLogin(LoginEvent event) {
        loginsSec.incrementAndGet();
    }

}
