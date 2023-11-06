<#import "builtin/debug.ftl" as debug>
<#import "builtin/html.ftlh" as html>

<@html.document>
<@html.head>
<script><#include 'jquery.js' parse=false></script>
<@html.highlighter />
<@html.expander />

<@html.css_styles />
<@html.css_fullframe />

</@html.head>
<@html.body>
<@html.box>

<@html.box_header>
<h1>System report (generated at ${.now?datetime})</h1>
</@html.box_header>
<@html.box_content>
<table>
<tbody>
<tr><th colspan="2" class="center">Memory</th></tr>
</tbody>
<tbody>
<tr><td class="right">used</td><td class="right">${MEM_USED?c} MB</td></tr>
<tr><td class="right">free</td><td class="right">${MEM_FREE?c} MB</td></tr>
<tr><td class="right">total</td><td class="right">${MEM_TOTAL?c} MB</td></tr>
<tr><td class="right">max</td><td class="right">${MEM_MAX?c} MB</td></tr>
</tbody>
<tbody>
<tr><th colspan="2" class="center">Host</th></tr>
</tbody>
<tbody>
<tr><td class="right">Java version</td><td class="right">${JAVA_VERSION}</td></tr>
<tr><td class="right">Hostname</td><td class="right">${HOSTNAME}</td></tr>
<tr><td class="right">OS</td><td class="right">${OS}</td></tr>
</tbody>
<#list IPs>
<tbody>
<tr><th colspan="2" class="center">IP</th></tr>
</tbody>
<tbody>
<#items as ip>
<tr><td colspan="2" class="right">${ip}</td></tr>
</#items>
</tbody>
</#list>
</table>

</@html.box_content>
<@html.box_footer class='noprint'>
<span class="button" id="expander_debug_pm" onclick="toggle_collapse('.expander_debug')">debug &plusmn;</span>
<div class="expander_debug " style="display: none;">
	<h1>HTTP input:</h1><div class="monospaced pre-wrap">${DescribeVariable(http.REQUEST, 'v', '|', '^')}</div>
</div>
</@html.box_footer>
</@html.box>
</@html.body>
</@html.document>
