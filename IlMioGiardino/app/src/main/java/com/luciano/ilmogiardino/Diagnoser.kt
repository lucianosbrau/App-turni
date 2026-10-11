package com.luciano.ilmogiardino

data class Question(val id: String, val text: String, val options: List<String>)

object Diagnoser {

    val questions = listOf(
        Question("leaves", "Come sono le foglie?", listOf("Ingiallite", "Con macchie", "Appassite/molle", "Arricciate", "Normali")),
        Question("soil", "Com'è il terreno al tatto?", listOf("Secco", "Molto umido/zuppo", "Normale")),
        Question("light", "Quanta luce riceve la pianta?", listOf("Poca (ombra)", "Luce indiretta", "Molta (sole diretto)")),
        Question("growth", "Come cresce la pianta?", listOf("Non cresce", "Cresce lenta", "Cresce bene")),
        Question("pests", "Vedi insetti o ragnetti?", listOf("Sì, sulle foglie", "Sì, nel terreno", "No"))
    )

    fun diagnose(answers: Map<String, Int>): String {
        val sb = StringBuilder()
        val leaves = answers["leaves"] ?: 4
        val soil = answers["soil"] ?: 2
        val light = answers["light"] ?: 1
        val growth = answers["growth"] ?: 2
        val pests = answers["pests"] ?: 2
        val problems = mutableListOf<String>()

        when (soil) {
            0 -> problems.add("Terriccio troppo secco: aumenta la frequenza di annaffiatura o controlla il drenaggio.")
            1 -> problems.add("Terriccio troppo bagnato: riduci le annaffiature e verifica che il vaso dreni bene; possibili marciumi radicali.")
        }
        when (leaves) {
            0 -> if (soil == 1) problems.add("Foglie gialle con terreno umido indicano ristagno idrico o carenza di azoto.")
                 else problems.add("Foglie ingiallite: possibile carenza di nutrienti (azoto/ferro) o eccesso di sole.")
            1 -> problems.add("Macchie sulle foglie: possibili funghi (macchia nera, oidio) o batteri. Rimuovi le foglie colpite e tratta con un fungicida.")
            2 -> problems.add("Foglie molli/appassite: la pianta soffre sete o calore eccessivo.")
            3 -> problems.add("Foglie arricciate: possibili parassiti (afidi, cocciniglie) o sbalzi termici/idrici.")
        }
        when (light) {
            0 -> problems.add("Luce insufficiente: sposta la pianta più vicino a una fonte luminosa.")
            2 -> if (leaves == 0) problems.add("Il sole diretto può bruciare le foglie: valuta una posizione con luce filtrata.")
        }
        when (growth) {
            0 -> problems.add("Cresce stentata: potrebbe servire rinvaso o concime bilanciato in stagione vegetativa.")
        }
        when (pests) {
            0 -> problems.add("Insetti sulle foglie: tratta con sapone molle potassico o olio di neem.")
            1 -> problems.add("Insetti nel terreno: usa un insetticida sistemico o nematodi utili.")
        }

        if (problems.isEmpty()) return "La pianta sembra in salute. Continua con le cure attuali: annaffiatura regolare, luce adeguata e controllo periodico di foglie e terreno."
        sb.append("Analisi del problema:\n")
        problems.forEach { sb.append("• ").append(it).append("\n") }
        sb.append("\nConsiglio: scatta anche una foto della foglia nella sezione Riconosci per un'analisi AI di specie e malattie.")
        return sb.toString()
    }
}
