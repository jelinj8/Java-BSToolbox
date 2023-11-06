<#ftl ns_prefixes={		
	"D":"http://ossj.tmobile.cz/xml/Common/v1-5",
	"ns2":"http://ossj.tmobile.cz/xml/Common-CBECore/v1-5",
	"ns3":"http://ossj.tmobile.cz/xml/Common-CBEBi/v1-5",
	"ns4":"http://ossj.tmobile.cz/xml/OrderManagement/v1-0",
	"ns5":"http://ossj.tmobile.cz/xml/Common-CBEDatatypes/v1-5",
	"ns6":"http://ossj.tmobile.cz/xml/Common-CBELocation/v1-5",
	"ns7":"http://ossj.tmobile.cz/xml/Common-CBEProduct/v1-5",
	"ns8":"http://ossj.tmobile.cz/xml/Common-CBEProductOffering/v1-5",
	"ns9":"http://provisioning.serviceresourcemanagement.tmobile.cz/ws/datatypes/somossjordermanagement",
	"ns10":"http://ossj.tmobile.cz/xml/Common-CBEService/v1-5",
	"ns11":"http://ossj.tmobile.cz/xml/Common-CBEResource/v1-5",
	"ns12":"http://ossj.tmobile.cz/xml/Common-CBEParty/v1-5",
	"ns13":"http://messaging.ei.tmobile.net/datatypes"
} 
>
<#import "builtin/debug.ftl" as debug>
<#import "builtin/html.ftlh" as html>


<#include "inc/macros.ftlh">

<@html.document>
<@html.head>
<script><#include 'jquery.js' parse=false></script>
<@html.expander />
<@html.highlighter />

<@html.css_styles />
<@html.css_fullframe />

</@html.head>
<@html.body>
<@html.box>

<@html.box_header>
<h1>Report (generated at ${.now?datetime})</h1>
</@html.box_header>
<@html.box_content>
<h2>Memory</h2>
<table>
<tdata>
<tr><td class="right">used</tr><td class="right">${MEM_MAX?c} MB</td></tr>
<tr><td class="right">free</tr><td class="right">${MEM_MAX?c} MB</td></tr>
<tr><td class="right">total</tr><td class="right">${MEM_MAX?c} MB</td></tr>
<tr><td class="right">max</tr><td class="right">${MEM_MAX?c} MB</td></tr>
</tdata>
</table>
<h2>Host</h2>
<table>
<tdata>
<tr><td class="right">Java version</tr><td class="right">${JAVA_VERSION}</td></tr>
<tr><td class="right">Hostname</tr><td class="right">${HOSTNAME} MB</td></tr>
<tr><td class="right">OS</tr><td class="right">${OS} MB</td></tr>
</tdata>
</table>

</@html.box_content>
<@html.box_footer class='noprint'>
<span class="button" id="expander_debug_pm" onclick="toggle_collapse('.expander_debug')">debug &plusmn;</span>
<div class="expander_debug " style="display: none;">
	<h1>HTTP input:</h1><div class="monospaced pre-wrap">${DescribeVariable(http.REQUEST, 'v', '|', '^')}</div>
	<#-- <h1>session:</h1><div class="monospaced pre-wrap">${DescribeVariable(http, 'v', '|', '^')}</div> -->
</div>
</@html.box_footer>
</@html.box>
</@html.body>
</@html.document>
