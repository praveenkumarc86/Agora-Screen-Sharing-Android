package io.agora.rtc.ss.app.newInterface;

import android.view.View;

//这里给出service的call back 接口
  public interface SurfaceReadyListener {
    void surfaceIsReady(View previewSurface);
  }