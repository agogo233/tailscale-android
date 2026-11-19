// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.viewModel

import com.tailscale.ipn.ui.util.LoadingIndicator
import com.tailscale.ipn.ui.util.set
import com.tailscale.ipn.ui.view.ErrorDialogType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

const val AUTH_KEY_LENGTH = 16

open class CustomLoginViewModel : IpnViewModel() {
  val errorDialog: StateFlow<ErrorDialogType?> = MutableStateFlow(null)
  val inProgress: StateFlow<Boolean> = MutableStateFlow(false)

  protected fun beginLogin() {
    inProgress.set(true)
    LoadingIndicator.start()
  }

  protected fun endLogin() {
    inProgress.set(false)
    LoadingIndicator.stop()
  }

  override fun onCleared() {
    if (inProgress.value) endLogin()
    super.onCleared()
  }
}

class LoginWithAuthKeyViewModel : CustomLoginViewModel() {
  // Sets the auth key and invokes the login flow
  fun setAuthKey(authKey: String, onSuccess: () -> Unit) {
    if (inProgress.value) return
    // The most basic of checks for auth key syntax
    if (authKey.isEmpty()) {
      errorDialog.set(ErrorDialogType.INVALID_AUTH_KEY)
      return
    }
    beginLogin()
    loginWithAuthKey(authKey) {
      endLogin()
      it.onFailure { errorDialog.set(ErrorDialogType.ADD_PROFILE_FAILED) }
      it.onSuccess { onSuccess() }
    }
  }
}

class LoginWithCustomControlURLViewModel : CustomLoginViewModel() {
  // Sets the custom control URL and invokes the login flow
  fun setControlURL(urlStr: String, authKey: String? = null, onSuccess: () -> Unit) {
    if (inProgress.value) return
    // Some basic checks that the entered URL is "reasonable".  The underlying
    // localAPIClient will use the default server if we give it a broken URL,
    // but we can make sure we can construct a URL from the input string and
    // ensure it has an http/https scheme
    val key = authKey?.takeIf { it.isNotBlank() }
    when (
        urlStr.startsWith("http", ignoreCase = true) && urlStr.contains("://") && urlStr.length > 7
    ) {
      false -> {
        errorDialog.set(ErrorDialogType.INVALID_CUSTOM_URL)
        return
      }
      true -> {
        beginLogin()
        loginWithCustomControlURL(urlStr, key) {
          endLogin()
          it.onFailure { errorDialog.set(ErrorDialogType.ADD_PROFILE_FAILED) }
          it.onSuccess { onSuccess() }
        }
      }
    }
  }
}
