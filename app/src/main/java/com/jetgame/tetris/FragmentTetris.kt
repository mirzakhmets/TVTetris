package com.jetgame.tetris

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jetgame.tetris.logic.*
import com.jetgame.tetris.ui.GameBody
import com.jetgame.tetris.ui.GameScreen
import com.jetgame.tetris.ui.PreviewGamescreen
import com.jetgame.tetris.ui.combinedClickable
import com.jetgame.tetris.ui.theme.ComposetetrisTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.tetrispower.step1structure.R
import androidx.fragment.app.Fragment

val BoardWidth: Dp = 300.dp
val BoardHeight: Dp = 600.dp

val BoardOffsetX: Dp = 100.dp
val BoardOffsetY: Dp = 20.dp

class FragmentTetris : Fragment(R.layout.fragment_tetris) {

    var viewModel: GameViewModel? = null
    var viewState: GameViewModel.ViewState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtil.transparentStatusBar(this.activity as Activity)
        SoundUtil.init(this.activity as Activity)

        activity?.setContent {
            ComposetetrisTheme {
                // A surface container using the 'background' color from the theme

                Surface(color = Color.Transparent//MaterialTheme.colors.background
                ) {

                    viewModel = viewModel<GameViewModel>()
                    viewState = viewModel!!.viewState.value

                    LaunchedEffect(key1 = Unit) {
                        while (isActive) {
                            delay(650L - 55 * (viewState!!.level - 1))
                            viewModel!!.dispatch(Action.GameTick)
                        }
                    }

                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(key1 = Unit) {
                        val observer = object : DefaultLifecycleObserver {
                            override fun onResume(owner: LifecycleOwner) {
                                viewModel!!.dispatch(Action.Resume)
                            }

                            override fun onPause(owner: LifecycleOwner) {
                                viewModel!!.dispatch(Action.Pause)
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }


                    GameBody(combinedClickable(
                        onMove = { direction: Direction ->
                            if (direction == Direction.Up) viewModel!!.dispatch(Action.Drop)
                            else viewModel!!.dispatch(Action.Move(direction))
                        },
                        onRotate = {
                            viewModel!!.dispatch(Action.Rotate)
                        },
                        onRestart = {
                            viewModel!!.dispatch(Action.Reset)
                        },
                        onPause = {
                            if (viewModel!!.viewState.value.isRuning) {
                                viewModel!!.dispatch(Action.Pause)
                            } else {
                                viewModel!!.dispatch(Action.Resume)
                            }
                        },
                        onMute = {
                            viewModel!!.dispatch(Action.Mute)
                        }
                    )) {
                        GameScreen(
                            //Modifier.size(BoardWidth, BoardHeight).zIndex(10.0f)
                            Modifier.size(BoardWidth, BoardHeight)
                        )
                    }
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        SoundUtil.release()
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ComposetetrisTheme {
        GameBody {
            PreviewGamescreen(Modifier.size(400.dp, 700.dp))
        }
    }
}