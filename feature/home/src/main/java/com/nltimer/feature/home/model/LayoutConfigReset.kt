package com.nltimer.feature.home.model

import com.nltimer.core.data.model.GridLayoutStyle
import com.nltimer.core.data.model.HomeLayoutConfig
import com.nltimer.core.data.model.LogLayoutStyle
import com.nltimer.core.data.model.MomentLayoutStyle
import com.nltimer.core.data.model.TimelineLayoutStyle
import com.nltimer.core.designsystem.theme.HomeLayout

fun HomeLayoutConfig.resetLayout(layout: HomeLayout): HomeLayoutConfig = when (layout) {
    HomeLayout.GRID -> copy(grid = GridLayoutStyle())
    HomeLayout.LOG -> copy(log = LogLayoutStyle())
    HomeLayout.TIMELINE_REVERSE -> copy(timeline = TimelineLayoutStyle())
    HomeLayout.MOMENT -> copy(moment = MomentLayoutStyle())
}
