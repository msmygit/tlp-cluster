package com.thelastpickle.tlpcluster.containers

import com.github.dockerjava.api.model.AccessMode
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.Docker
import com.thelastpickle.tlpcluster.Utils
import com.thelastpickle.tlpcluster.VolumeMapping
import com.thelastpickle.tlpcluster.configuration.ServerType

import com.thelastpickle.tlpcluster.configuration.toEnv
import org.apache.logging.log4j.kotlin.logger


/**
 * This is currently flawed in that it only allows for SSH'ing to Cassandra
 */
class Pssh(val context: Context, val sshKey: String) {
    private val docker = Docker(context)
    private val dockerImageTag = "thelastpickle/tlp-cluster_pssh"
    private val volumeMappings = mutableListOf(
            VolumeMapping(sshKey, "/root/.ssh/aws-private-key", AccessMode.ro),
            VolumeMapping(context.cwdPath, "/local", AccessMode.rw))
    private val provisionCommand = "cd provisioning; chmod +x install.sh; sudo ./install.sh"

    val log = logger()
    init {
        docker.pullImage("ubuntu:bionic")
    }

    fun buildContainer() : String {
        return docker.buildContainer("DockerfileSSH", dockerImageTag)
    }

    fun copyProvisioningResources() : Result<String> {
        return execute("copy_provisioning_resources.sh", "")
    }

    fun provisionNode(nodeType: String) : Result<String> {
        return execute("parallel_ssh.sh", "$provisionCommand $nodeType")
    }

    fun startService(nodeType: String) : Result<String> {
        return execute("parallel_ssh.sh", "sudo service $nodeType start")
    }

    fun execute(scriptName: String, scriptCommand: String) : Result<String> {
        val scriptFile = Utils.resourceToTempFile("containers/$scriptName", context.cwdPath)
        val scriptPathInContainer = "/local/$scriptName"
        val containerCommands = mutableListOf("/bin/sh", scriptPathInContainer)

        if (scriptCommand.isNotEmpty()) {
            containerCommands.add(scriptCommand)
        }

        volumeMappings.add(VolumeMapping(scriptFile.absolutePath, scriptPathInContainer, AccessMode.rw))

        val hosts = context.tfstate.getHosts(ServerType.Cassandra).toEnv()
        log.info("Starting container with $hosts")

        return docker.runContainer(dockerImageTag, containerCommands, volumeMappings,"", mutableListOf(hosts))
    }
}