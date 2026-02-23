<#import "template.ftl" as layout>

<@layout.registrationLayout displayInfo=false; section>

    <#if section == "header">
        <div class="modern-header-wrapper">
            <img src="${url.resourcesPath}/img/vima-logo-main.DiciI4hf.png"
                 class="modern-logo"
                 alt="VIMA Logo"/>
            <h2 class="modern-title">${msg("updatePasswordTitle")}</h2>
            <p class="modern-subtitle">Please update your password</p>
        </div>

    <#elseif section == "form">
        <div class="modern-container">
            <div class="modern-card">
                ${form}
            </div>
        </div>
    </#if>

</@layout.registrationLayout>