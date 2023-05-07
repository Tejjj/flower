/*
 *  Copyright (C) 2023 Rajesh Hadiya
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.example.xml_app.fragment.detail

//import com.example.xml_app.fragment.ProfileDetailFragmentArgs
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.xml_app.R
import com.example.xml_app.database.entity.Image
import com.example.xml_app.databinding.FragmentProfileDetailBinding
import com.example.xml_app.util.LoadResourceFrom
import com.example.xml_app.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileDetailFragment : Fragment(R.layout.fragment_profile_detail) {

    private val TAG = "ProfileDetailFragment"

    private val args: ProfileDetailFragmentArgs by navArgs()

    private val viewModel: ProfileDetailViewModel by viewModels()

    private lateinit var lBinding: FragmentProfileDetailBinding

    private lateinit var profileImage: Image
    private var isNetworkLoad: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lBinding = FragmentProfileDetailBinding.bind(view)
        val profileId = args.profileId?.trim()?.toLong() ?: 0
        getProfileInfo(profileId)
        initClickListener()
    }

    override fun onResume() {
        super.onResume()
        updateNetworkLoadBtnVisibility()
    }
    private fun initClickListener() {
        lBinding.loadfromNetwork.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    isNetworkLoad = true
                    viewModel.getImage(profileImage.id.toLong(), loadFrom = LoadResourceFrom.Network)
                }
            }
        }

        lBinding.profileURL.highlightColor = Color.BLUE
        lBinding.profileURL?.setOnClickListener { profileURL ->
            try {
                var openURL = Intent(android.content.Intent.ACTION_VIEW)
                openURL.data = Uri.parse("${profileURL.tag.toString()}")
                startActivity(openURL)
            } catch (exception: Exception) {
                Log.d(TAG, " exception occured : $exception ")
            }

        }
    }

    private fun getProfileInfo(profileId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                isNetworkLoad = false
                viewModel.getImage(profileId, loadFrom = LoadResourceFrom.Db)
            }
        }
        // progressBar

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.image.collectLatest { profileState ->
                    when (profileState) {
                        is UiState.Loading -> {
                            showProgressBar()
                        }

                        is UiState.Success -> {
                            profileImage = (profileState as UiState.Success).data
                            updateProfileUI()
                        }

                        UiState.Empty -> {
                            showNoUserInfoFound(getString(R.string.no_details_found))
                        }

                        is UiState.Error -> {
                            showNoUserInfoFound(getString(R.string.loading_error_msg))
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadFrom.collectLatest { loadingFrom ->
                    when(loadingFrom) {
                        LoadResourceFrom.Db -> {
                            isNetworkLoad = false
                        }

                        LoadResourceFrom.Network -> {
                            isNetworkLoad = true
                        }
                    }
                    updateNetworkLoadBtnVisibility()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateProfileUI() {
        hideProgressBar()

        profileImage?.let {
            lBinding.profileImage.load(profileImage.downloadUrl) {
                crossfade(300)
                transformations(RoundedCornersTransformation(4f))
            }
            //updateProfileUI()
            lBinding.authorName.text = it.author
            //lBinding.profileURL.setPaintFlags(lBinding.authorName.getPaintFlags() or Paint.UNDERLINE_TEXT_FLAG)
            lBinding.profileURL.paint?.isUnderlineText = true
            lBinding.profileURL.paint.underlineColor = Color.BLUE
            lBinding.profileURL.text = it.url
            lBinding.profileURL.tag = it.url
        }
        updateNetworkLoadBtnVisibility()
    }

    private fun updateNetworkLoadBtnVisibility() {
        lBinding.imageDestInfo.visibility = if (isNetworkLoad) View.GONE else View.VISIBLE
    }

    private fun showProgressBar() {
        lBinding.profileDetailScreen.visibility = View.GONE
        lBinding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        lBinding.profileDetailScreen.visibility = View.VISIBLE
        lBinding.progressBar.visibility = View.GONE
    }

    private fun showNoUserInfoFound(message: String) {
        lBinding.profileDetailScreen.visibility = View.GONE
        lBinding.progressBar.visibility = View.GONE
        lBinding.tvErrMsg.text = message
        lBinding.noInfoMsg.visibility = View.VISIBLE
    }


}