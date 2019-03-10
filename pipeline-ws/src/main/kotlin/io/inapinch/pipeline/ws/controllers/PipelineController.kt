package io.inapinch.pipeline.ws.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.google.common.collect.Maps
import io.inapinch.pipeline.*
import io.inapinch.pipeline.db.PipelineDao
import io.inapinch.pipeline.operations.CommandUsage
import io.inapinch.pipeline.ws.WebApplication
import io.javalin.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

data class Status(val message: String, val timestamp: LocalDateTime = LocalDateTime.now())

data class PipelineError(val error: String)

private val LOG = LoggerFactory.getLogger(WebApplication::class.java)

class PipelineController(private val mapper: ObjectMapper,
                         private val dao: PipelineDao,
                         private val manager: OperationsManager,
                         private val commands: Supplier<List<CommandUsage>> = Suppliers.memoize { CommandUsage.all() }) {

    fun newRequest(context: Context) {
        val request : PipelineRequest = mapper.readValue(context.body())

        context.header("Location", manager.enqueue(request))
        context.status(202)
    }

    fun newRequestFromCommandLanguage(context: Context) {
        val request : PipelineCLRequest = mapper.readValue(context.body())

        context.header("Location", manager.enqueue(request.toPipelineRequest(mapper)))
        context.status(202)
    }

    fun pipelineStatus(context: Context) {
        context.json(manager.status(context.param("id") as String))
    }

    fun pipelineRequest(context: Context) {
        val uuid = context.param("id") as String
        context.json(dao.request(uuid).orElse(null) ?: PipelineStatus("Not Found", uuid))
    }

    fun error(e: Exception, context: Context) {
        LOG.error(e.localizedMessage, e)
        context.json(PipelineError(e.message ?: e.localizedMessage))
    }

    fun status(context: Context) {
        context.json(Status("ok"))
    }

    fun commands(context: Context) {
        context.json(commands.get())
    }
}