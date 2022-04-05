<#assign dateTimeFormatString="yyyy-MM-dd HH:mm:ss" />

<#macro objectToJson object>
    <@compress single_line=true>
        <#if object?is_hash || object?is_hash_ex>
            <#assign first="true">
        {
            <#list object?keys as key>
                <#if first="false">,</#if>
                <#assign foo = key />
                <#assign value><@objectToJson object=object[key]!"" /></#assign>
            "${key}": ${value?trim}
                <#assign first="false">
            </#list>
        }
        <#elseif object?is_enumerable>
            <#assign first="true">
        [
            <#list object as item>
                <#if first="false">,</#if>
                <#assign value><@objectToJson object=item /></#assign>
            ${value?trim}
                <#assign first="false">
            </#list>
        ]
        <#else>
        	<#if !(object??)>
			null
			<#elseif object?is_boolean>
			${object?c}
			<#elseif object?is_number>
			${object?c}
			<#elseif object?is_date_only>
			"${object?string["yyyy-MM-dd"]}"
			<#elseif object?is_datetime>
			"${object?iso_local}"
			<#elseif object?is_time>
			"${object?string["HH:mm:ss"]}"
			<#elseif object?is_method>
			"METHOD"
			<#else>
			"${object?trim?replace('\\','\\\\"')?replace('"','\\"')?replace('\t','\\t')?replace('\b','\\b')?replace('\f','\\f')?replace('\n','\\n')?replace('\r','\\r')}"
			</#if>
        </#if>
    </@compress>
</#macro>

<#macro objectToFormattedJson object pad=''>
        <#if object?is_hash || object?is_hash_ex>
${pad}{
            <#list object?keys as key>
                <#assign value><@objectToFormattedJson object=object[key]!"" pad=('\t'+pad)/></#assign>
${pad}	"${key}": ${value?trim}<#if !key?is_last>,</#if>
            </#list>
${pad}}
        <#elseif object?is_enumerable>
${pad}[
            <#list object as item>
                <#assign value><@objectToFormattedJson object=item  pad=('\t'+pad)/></#assign>
${pad}	${value?trim}<#if !item?is_last>,</#if>
            </#list>
${pad}]
        <#else>
        	<#if !(object??)>
${pad}	null
			<#elseif object?is_boolean>
${pad}	${object?c}
			<#elseif object?is_number>
${pad}	${object?c}
			<#elseif object?is_date_only>
${pad}	"${object?string["yyyy-MM-dd"]}"
			<#elseif object?is_datetime>
${pad}	"${object?string[dateTimeFormatString]}"
			<#elseif object?is_time>
${pad}	"${object?string["HH:mm:ss"]}"
			<#elseif object?is_method>
${pad}	"METHOD"
			<#else>
${pad}	"${object?trim?replace('\\','\\\\"')?replace('"','\\"')}"
			</#if>
        </#if>
</#macro>