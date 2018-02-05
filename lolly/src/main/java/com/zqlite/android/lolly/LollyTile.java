/*
 * Copyright [2016] [qinglian.zhang]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.zqlite.android.lolly;

import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.annotation.RequiresApi;
import android.util.Log;

/**
 * @author scott
 */

@RequiresApi(api = Build.VERSION_CODES.N)
public abstract  class LollyTile extends TileService {
    private boolean flag = false;
    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if(!flag){
            tile.setState(Tile.STATE_INACTIVE);
        }else {
            tile.setState(Tile.STATE_ACTIVE);
        }
        tile.updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        Log.d("scott","onStopListening");
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        Log.d("scott","onTileAdded");
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        Log.d("scott","onTileRemoved");
    }

    @Override
    public void onClick() {
        super.onClick();
        Tile tile = getQsTile();
        flag = !flag;
        if(!flag){
            Lolly.Companion.hideLolly(this);
            tile.setState(Tile.STATE_INACTIVE);
        }else {
            Lolly.Companion.showLolly(this,getTags());
            tile.setState(Tile.STATE_ACTIVE);
        }
        tile.updateTile();
    }
    public abstract String[] getTags();
}
