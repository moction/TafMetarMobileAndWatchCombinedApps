package com.example.tafmetar.mobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FavoritesViewModel = viewModel()) {
    val favorites by viewModel.favorites.collectAsState(initial = emptySet())
    var newIcao by remember { mutableStateOf("") }

    fun submit() {
        if (newIcao.isNotBlank()) {
            viewModel.addStation(newIcao)
            newIcao = ""
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "STATIONS",
            style = MaterialTheme.typography.labelLarge,
            color = DimGrey,
            letterSpacing = 3.sp
        )

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newIcao,
                onValueChange = { newIcao = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                placeholder = { Text("OACI", color = DimGrey) },
                singleLine = true,
                // Un code OACI fait 4 caractères : clavier en majuscules et validation directe
                // au clavier, pour éviter d'avoir à viser le bouton.
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = DimGrey,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = { submit() }) {
                Text("AJOUTER", color = Color.White, letterSpacing = 1.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (favorites.isEmpty()) {
            Text(
                "Aucune station suivie.",
                style = MaterialTheme.typography.bodyMedium,
                color = DimGrey,
                modifier = Modifier.padding(top = 24.dp)
            )
        }

        LazyColumn {
            items(favorites.toList().sorted()) { icao ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        icao,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        ),
                        color = Color.White
                    )
                    TextButton(onClick = { viewModel.removeStation(icao) }) {
                        Text("RETIRER", color = DimGrey, letterSpacing = 1.sp)
                    }
                }
                HorizontalDivider(color = Color(0xFF2A2A2A))
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            "Les relevés sont récupérés ici puis poussés vers la montre.",
            style = MaterialTheme.typography.bodySmall,
            color = DimGrey
        )
    }
}
