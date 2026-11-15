package se.kodhast.reseapoteket.ui

import se.kodhast.reseapoteket.data.common.AppErrors
import se.kodhast.reseapoteket.data.common.DataError
import se.kodhast.reseapoteket.ui.theme.ReseApoteketTheme
import se.kodhast.ui.testing.ContractEnvironment

/**
 * Det ramarnas kontraktstester (android-ui `ui-testing`: `runListScreenContract`, `runEditScreenContract`,
 * `runDetailScreenContract`) behöver av appen: temat, felöversättningen och ett fel där "Försök igen"
 * hjälper (NFR-1, NFR-2, NFR-5). En gång för alla skärmtester.
 */
val contracts = ContractEnvironment(AppErrors, DataError.Offline) { ReseApoteketTheme(content = it) }
