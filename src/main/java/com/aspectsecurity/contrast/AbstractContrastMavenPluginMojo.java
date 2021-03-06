package com.aspectsecurity.contrast;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.models.AgentType;
import com.contrastsecurity.sdk.ContrastSDK;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.awt.peer.SystemTrayPeer;
import java.io.File;
import java.io.IOException;
import java.util.Date;

abstract class AbstractContrastMavenPluginMojo extends AbstractMojo {

    @Component
    protected MavenProject project;

    @Parameter(property = "username", required = true)
    protected String username;

    @Parameter(property = "apiKey", required = true)
    protected String apiKey;

    @Parameter(property = "serviceKey", required = true)
    protected String serviceKey;

    @Parameter(property = "apiUrl")
    protected String apiUrl;

    @Parameter(property = "orgUuid", required = true)
    protected String orgUuid;

    @Parameter(property = "appName", required = true)
    protected String appName;

    @Parameter(property = "minSeverity", defaultValue = "Medium")
    protected String minSeverity;

    @Parameter(property = "serverName", required = true)
    protected String serverName;

    @Parameter(property = "jarPath")
    protected String jarPath;

    // Start time we will look for
    protected static Date verifyDateTime;

    private static final String AGENT_NAME = "contrast.jar";

    public void execute() throws MojoExecutionException {
    }

    ContrastSDK connectToTeamServer() throws MojoExecutionException {
        try {
            if (!StringUtils.isEmpty(apiUrl)) {
                return new ContrastSDK(username, serviceKey, apiKey, apiUrl);
            } else {
                return new ContrastSDK(username, serviceKey, apiKey);
            }
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("Unable to connect to TeamServer. Please check your maven settings.", e);
        }
    }

    File installJavaAgent(ContrastSDK connection) throws MojoExecutionException {
        byte[] javaAgent;
        File agentFile;

        if (StringUtils.isEmpty(jarPath)) {
            getLog().info("No jar path was configured. Downloading the latest contrast.jar...");

            try {
                javaAgent = connection.getAgent(AgentType.JAVA, orgUuid);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to download the latest java agent.", e);
            } catch (UnauthorizedException e) {
                throw new MojoExecutionException("Unable to retrieve the latest java agent due to authorization.", e);
            }

            // Save the jar to the 'target' directory
            agentFile = new File(project.getBuild().getDirectory() + File.separator + AGENT_NAME);

            try {
                FileUtils.writeByteArrayToFile(agentFile, javaAgent);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to save the latest java agent.", e);
            }

            getLog().info("Saved the latest java agent to " + agentFile.getAbsolutePath());

        } else {
            getLog().info("Using configured jar path " + jarPath);

            agentFile = new File(jarPath);

            if (!agentFile.exists()) {
                throw new MojoExecutionException("Unable to load the local Java agent from " + jarPath);
            }

            getLog().info("Loaded the latest java agent from " + jarPath);

        }

        return agentFile;
    }
}