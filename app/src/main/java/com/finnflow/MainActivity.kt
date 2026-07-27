package com.finnflow

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.finnflow.ui.MainNavHost
import com.finnflow.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    // Process-level ON_STOP fires only when the whole app leaves the foreground — not on
    // rotation or other config changes — so App Lock re-arms exactly when the user walks
    // away, without re-prompting mid-session.
    private val relockOnBackground = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            mainViewModel.onLocked()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ProcessLifecycleOwner.get().lifecycle.addObserver(relockOnBackground)
        setContent {
            MainNavHost(mainViewModel)
        }
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(relockOnBackground)
        super.onDestroy()
    }
}
