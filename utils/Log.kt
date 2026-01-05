object Log {

    private const val TEXT_ERROR = "\u001B[31m"
    private const val TEXT_DEBUG = "\u001b[34m"
    private const val TEXT_INFO = "\u001b[35m"
    private const val TEXT_WARNING = "\u001b[36m"
    private const val TEXT_RESET = "\u001B[0m"
    private const val TEXT_STYLE = "\u001B[1m"
    fun v(message: String) = println("V/ : $message $TEXT_RESET")
    fun w(message: String) = println("$TEXT_WARNING W/ : $message $TEXT_RESET")
    fun i(message: String) = println("$TEXT_INFO I/ : $message $TEXT_RESET")
    fun d(message: String) = println("$TEXT_DEBUG D/ : $message $TEXT_RESET")
    fun e(message: String) = println("${TEXT_STYLE}${TEXT_ERROR} E/: $message $TEXT_RESET")

    fun logData(data: Map<String, Any>) {
        for ((key, value) in data) {
            d("[$key] [$value]")
        }
    }
}