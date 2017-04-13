<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<build>
    <buildType id="${trigger.buildTypeId}"/>
<#if changeId??>
    <lastChanges>
        <change id="${changeId}"/>
    </lastChanges>
</#if>
</build>