<#assign dateTimeFormatString="yyyy-MM-dd HH:mm:ss" />

<#macro objectToJson object>
    <@compress single_line=true>
        <#if object?is_hash_ex>
            <#assign first="true">
        {
            <#list object as key, value>
                <#if first="false">,</#if>
                <#assign res><@objectToJson value!"" /></#assign>
            "${key}": ${res?trim}
                <#assign first="false">
            </#list>
        }
        <#elseif object?is_node>
        {
        	"NODE"
        }
        <#elseif object?is_hash>
            <#assign first="true">
        {
			"SIMPLE HASH (not supported)"
            <#-- <#list object as key, value>
                <#if first="false">,</#if>
                <#assign res><@objectToJson value!"" /></#assign>
            "${key}": ${res?trim}
                <#assign first="false">
            </#list> -->
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
			<#elseif object?is_macro>
			"MACRO"
			<#else>
			"${object?trim?replace('\\','\\\\"')?replace('"','\\"')?replace('\t','\\t')?replace('\b','\\b')?replace('\f','\\f')?replace('\n','\\n')?replace('\r','\\r')}"
			</#if>
        </#if>
    </@compress>
</#macro>

<#macro objectToFormattedJson object pad=''>
        <#if object?is_hash_ex>
${pad}{
            <#list object as key, val>
                <#assign value><@objectToFormattedJson object=val!"" pad=('\t'+pad)/></#assign>
${pad}	"${key}": ${value?trim}<#if !key?is_last>,</#if>
            </#list>
${pad}}
        <#elseif object?is_node>
			<#switch object?node_type>
				<#case "text">
${pad}	"${object?trim?replace('\\','\\\\"')?replace('"','\\"')}"
				<#break>
				<#case "attribute">
${pad}	@${object?node_name}:"${object!""?trim?replace('\\','\\\\"')?replace('"','\\"')}"
				<#break>
				<#case "comment">
${pad}	<!-- ${object} -->
				<#break>
				<#case "element">
					<#if object?children?size==1>
                <#assign value><@objectToFormattedJson object=object?children[0]  pad=('\t'+pad)/></#assign>
${pad}${object?node_name}:${value?trim}
					<#else>
${pad}${object?node_name}:[            
            <#list object?children as item>
                <#assign value><@objectToFormattedJson object=item  pad=('\t'+pad)/></#assign>
${pad}	${value?trim}<#if !item?is_last>,</#if>
            </#list>
${pad}]
					</#if>
				<#break>
				<#default>
${pad}	NODE_type:"${object?node_type}"
			</#switch>
        <#elseif object?is_hash>
${pad}{
<#-- ${pad}	"SIMPLE HASH (not supported), class: ${identifyObjectType(object)}" -->
${pad}	[
            <#list object as item>
                <#assign value><@objectToFormattedJson object=item  pad=('\t'+pad)/></#assign>
${pad}	${value?trim}<#if !item?is_last>,</#if>
            </#list>
${pad}	]
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
			<#elseif object?is_macro>
${pad}	"MACRO"
			<#else>
${pad}	"${object?trim?replace('\\','\\\\"')?replace('"','\\"')}"
			</#if>
        </#if>
</#macro>