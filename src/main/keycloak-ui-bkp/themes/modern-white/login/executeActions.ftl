<#import "template.ftl" as layout>

<@layout.registrationLayout displayInfo=false; section>

    <#if section = "header">
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <img src="${url.resourcesPath}/img/vima-logo-main.DiciI4hf.png"
                     class="modern-logo"
                     alt="VIMA Logo"/>
                ${form}
            </div>
        </div>
    </#if>

</@layout.registrationLayout>
