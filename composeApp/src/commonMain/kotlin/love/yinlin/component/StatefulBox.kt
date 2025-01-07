package love.yinlin.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ylcs_kmp.composeapp.generated.resources.Res
import ylcs_kmp.composeapp.generated.resources.empty_state_string
import ylcs_kmp.composeapp.generated.resources.loading_state_string
import ylcs_kmp.composeapp.generated.resources.network_error_retry_string
import ylcs_kmp.composeapp.generated.resources.network_error_state_string
import ylcs_kmp.composeapp.generated.resources.state_empty
import ylcs_kmp.composeapp.generated.resources.state_loading
import ylcs_kmp.composeapp.generated.resources.state_network_error

enum class BoxState {
	LOADING,
	CONTENT,
	EMPTY,
	NETWORK_ERROR
}

@Composable
private fun LoadingBox() {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(20.dp)
	) {
		Image(
			modifier = Modifier.height(200.dp),
			painter = painterResource(Res.drawable.state_loading),
			contentDescription = null
		)
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(20.dp)
		) {
			CircularProgressIndicator(modifier = Modifier.size(32.dp))
			Text(text = stringResource(Res.string.loading_state_string))
		}
	}
}

@Composable
private fun EmptyBox() {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(20.dp)
	) {
		Image(
			modifier = Modifier.size(200.dp),
			painter = painterResource(Res.drawable.state_empty),
			contentDescription = null,
		)
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(20.dp)
		) {
			MiniIcon(Icons.Filled.Error)
			Text(text = stringResource(Res.string.empty_state_string))
		}
	}
}

@Composable
private fun NetWorkErrorBox(retry: (() -> Unit)? = null) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(20.dp)
	) {
		Image(
			modifier = Modifier.size(200.dp),
			painter = painterResource(Res.drawable.state_network_error),
			contentDescription = null,
		)
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(20.dp)
		) {
			MiniIcon(
				imageVector = Icons.Filled.WifiOff,
				color = MaterialTheme.colorScheme.error
			)
			Text(
				text = stringResource(Res.string.network_error_state_string),
				color = MaterialTheme.colorScheme.error
			)
		}
		Button(onClick = { retry?.invoke() }) {
			Text(text = stringResource(Res.string.network_error_retry_string))
		}
	}
}

@Composable
fun StatefulBox(
	state: BoxState,
	retry: (() -> Unit)? = null,
	modifier: Modifier = Modifier,
	content: @Composable () -> Unit
) {
	Crossfade(
		targetState = state,
		modifier = modifier,
	) {
		Box(
			modifier = Modifier.fillMaxSize(),
			contentAlignment = Alignment.Center
		) {
			when (it) {
				BoxState.CONTENT -> content()
				BoxState.LOADING -> LoadingBox()
				BoxState.EMPTY -> EmptyBox()
				BoxState.NETWORK_ERROR -> NetWorkErrorBox(retry)
			}
		}
	}
}