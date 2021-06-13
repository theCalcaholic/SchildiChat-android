/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home

// import im.vector.app.features.grouplist.GroupListFragment

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.observeK
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.startSharePlainTextIntent
import im.vector.app.databinding.FragmentHomeDrawerBinding
import im.vector.app.features.call.conference.JitsiService
import im.vector.app.features.login.PromptSimplifiedModeActivity
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.settings.VectorSettingsActivity
import im.vector.app.features.usercode.UserCodeActivity
import im.vector.app.features.workers.signout.SignOutUiWorker
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.toMatrixItem
import timber.log.Timber
import javax.inject.Inject

class HomeDrawerFragment @Inject constructor(
        private val session: Session,
        private val jitsiService: JitsiService,
        private val vectorPreferences: VectorPreferences,
        private val avatarRenderer: AvatarRenderer
) : VectorBaseFragment<FragmentHomeDrawerBinding>() {

    private lateinit var sharedActionViewModel: HomeSharedActionViewModel

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeDrawerBinding {
        return FragmentHomeDrawerBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedActionViewModel = activityViewModelProvider.get(HomeSharedActionViewModel::class.java)

        // TODO: Make configurable via learn path
//        if (savedInstanceState == null) {
//            replaceChildFragment(R.id.homeDrawerGroupListContainer, SpaceListFragment::class.java)
//        }
        setupSupportButton(requireContext())
        session.getUserLive(session.myUserId).observeK(viewLifecycleOwner) { optionalUser ->
            val user = optionalUser?.getOrNull()
            if (user != null) {
                avatarRenderer.render(user.toMatrixItem(), views.homeDrawerHeaderAvatarView)
                views.homeDrawerUsernameView.text = user.displayName
                views.homeDrawerUserIdView.text = user.userId
            }
        }
        // Profile
        views.homeDrawerHeader.debouncedClicks {
            sharedActionViewModel.post(HomeActivitySharedAction.CloseDrawer)
            navigator.openSettings(requireActivity(), directAccess = VectorSettingsActivity.EXTRA_DIRECT_ACCESS_GENERAL)
        }
        // Settings
        views.homeDrawerHeaderSettingsView.debouncedClicks {
            sharedActionViewModel.post(HomeActivitySharedAction.CloseDrawer)
            navigator.openSettings(requireActivity())
        }
        // Sign out
        views.homeDrawerHeaderSignoutView.debouncedClicks {
            sharedActionViewModel.post(HomeActivitySharedAction.CloseDrawer)
            SignOutUiWorker(requireActivity()).perform()
        }

        views.homeDrawerQRCodeButton.debouncedClicks {
            UserCodeActivity.newIntent(requireContext(), sharedActionViewModel.session.myUserId).let {
                val options =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                                requireActivity(),
                                views.homeDrawerHeaderAvatarView,
                                ViewCompat.getTransitionName(views.homeDrawerHeaderAvatarView) ?: ""
                        )
                startActivity(it, options.toBundle())
            }
        }

        views.homeDrawerInviteFriendButton.debouncedClicks {
            session.permalinkService().createPermalink(sharedActionViewModel.session.myUserId)?.let { permalink ->
                val text = getString(R.string.invite_friends_text, permalink)

                startSharePlainTextIntent(
                        fragment = this,
                        activityResultLauncher = null,
                        chooserTitle = getString(R.string.invite_friends),
                        text = text,
                        extraTitle = getString(R.string.invite_friends_rich_title)
                )
            }
        }

        // Debug menu
        views.homeDrawerHeaderDebugView.isVisible = BuildConfig.DEBUG && vectorPreferences.developerMode()
        views.homeDrawerHeaderDebugView.debouncedClicks {
            sharedActionViewModel.post(HomeActivitySharedAction.CloseDrawer)
            navigator.openDebug(requireActivity())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SC: settings migration
        vectorPreferences.scPreferenceUpdate()
        // SC-Easy mode prompt
        PromptSimplifiedModeActivity.showIfRequired(requireContext(), vectorPreferences)
    }

    fun setupSupportButton(c: Context) {
        val pkgManager = c.packageManager
        var appInfo: ApplicationInfo? = null
        try {
            appInfo = pkgManager.getApplicationInfo("org.jitsi.meet", PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.i("Could not find package org.jitsi.meet")
        }
        if (appInfo == null) {
            views.getSupportButton.alpha = 0.25F
            views.getSupportHeader.text = getString(R.string.get_support_feature_jitsi_missing)
        } else {
            views.getSupportHeader.text = getString(R.string.get_support)
            views.getSupportButton.setColorFilter(Color.argb(0, 0, 0, 0))
            views.getSupportButton.setImageDrawable(pkgManager.getApplicationIcon("org.jitsi.meet"))
            views.getSupportButton.onClick {
                // TODO: Respect user settings

                val jitsiConferenceURI = "https://meet.jit.si/${java.util.UUID.randomUUID()}"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(jitsiConferenceURI))
                startActivity(browserIntent)
            }
        }
    }
}
