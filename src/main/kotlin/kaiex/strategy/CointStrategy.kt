package kaiex.strategy

import org.slf4j.LoggerFactory
import java.util.logging.Logger

class CointStrategy(val symbol1: String,
                    val symbol2: String) : Strategy("CointStrategy($symbol1,$symbol2)") {

    private val log: org.slf4j.Logger = LoggerFactory.getLogger(javaClass.simpleName)
}