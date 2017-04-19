<#if vcs.type == "git">
    <#include "*/createGitVcsRoot.ftl"/>
</#if>
<#if vcs.type == "hg">
    <#include "*/createHgVcsRoot.ftl"/>
</#if>