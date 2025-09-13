package org.lyxith.lyxithconfig;


import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.lyxith.lyxithconfig.api.LyXithConfigAPI;
import org.lyxith.lyxithconfig.api.LyXithConfigAPIImpl;

public class LyXithConfigEntryPoint implements PreLaunchEntrypoint {

    public static final LyXithConfigAPI API = new LyXithConfigAPIImpl();

    @Override
    public void onPreLaunch() {

    }
}