<#if vcs.type == "git">
    <#include "*/createGitVcsRoot.ftl"/>
</#if>
<#if vcs.type == "hg" || vcs.type == "mercurial">
    <#include "*/createHgVcsRoot.ftl"/>
</#if>