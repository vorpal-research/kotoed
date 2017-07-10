<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<vcs-root id="${vcs.id}" name="${vcs.name}" vcsName="jetbrains.git">
    <project id="${vcs.projectId}"/>
    <properties count="9">
        <property name="url" value="${vcs.url}"/>

        <property name="agentCleanFilesPolicy" value="ALL_UNTRACKED"/>
        <property name="agentCleanPolicy" value="ON_BRANCH_CHANGE"/>
        <property name="authMethod" value="ANONYMOUS"/>
        <property name="branch" value="refs/heads/master"/>
        <property name="ignoreKnownHosts" value="true"/>
        <property name="submoduleCheckout" value="CHECKOUT"/>
        <property name="useAlternates" value="true"/>
        <property name="usernameStyle" value="USERID"/>
    </properties>
</vcs-root>