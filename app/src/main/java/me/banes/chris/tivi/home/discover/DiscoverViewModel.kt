/*
 * Copyright 2017 Google, Inc.
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
 */

package me.banes.chris.tivi.home.discover

import android.arch.lifecycle.MutableLiveData
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.plusAssign
import me.banes.chris.tivi.SharedElementHelper
import me.banes.chris.tivi.data.entities.TiviShow
import me.banes.chris.tivi.home.HomeFragmentViewModel
import me.banes.chris.tivi.home.HomeNavigator
import me.banes.chris.tivi.tmdb.TmdbManager
import me.banes.chris.tivi.trakt.TraktManager
import me.banes.chris.tivi.trakt.calls.PopularCall
import me.banes.chris.tivi.trakt.calls.TrendingCall
import me.banes.chris.tivi.util.AppRxSchedulers
import me.banes.chris.tivi.util.NetworkDetector
import timber.log.Timber
import javax.inject.Inject

class DiscoverViewModel @Inject constructor(
    schedulers: AppRxSchedulers,
    private val popularCall: PopularCall,
    private val trendingCall: TrendingCall,
    traktManager: TraktManager,
    tmdbManager: TmdbManager,
    private val networkDetector: NetworkDetector
) : HomeFragmentViewModel(traktManager) {

    val data = MutableLiveData<DiscoverViewState>()

    init {
        disposables += Flowables.combineLatest(
                trendingCall.data(0),
                popularCall.data(0),
                tmdbManager.imageProvider,
                ::DiscoverViewState)
                .observeOn(schedulers.main)
                .subscribe(data::setValue, Timber::e)

        refresh()
    }

    private fun refresh() {
        disposables += networkDetector.waitForConnection()
                .subscribe({ onRefresh() }, Timber::e)
    }

    private fun onRefresh() {
        launchWithParent {
            try {
                popularCall.refresh(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Error while refreshing popular shows")
            }
        }
        launchWithParent {
            try {
                trendingCall.refresh(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Error while refreshing trending shows")
            }
        }
    }

    fun onTrendingHeaderClicked(navigator: HomeNavigator, sharedElementHelper: SharedElementHelper? = null) {
        navigator.showTrending(sharedElementHelper)
    }

    fun onPopularHeaderClicked(navigator: HomeNavigator, sharedElementHelper: SharedElementHelper? = null) {
        navigator.showPopular(sharedElementHelper)
    }

    fun onItemPostedClicked(navigator: HomeNavigator, show: TiviShow, sharedElements: SharedElementHelper?) {
        navigator.showShowDetails(show, sharedElements)
    }
}
