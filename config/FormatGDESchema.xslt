<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
		<xsl:output method="html" version="1.0" encoding="UTF-8" indent="yes"/>
	<!--This xslt is both for ack message and for ack message details, the first section is for ack message and  the second one is for ack message details-->
	<!-- template for messageAck-->
	<xsl:template match="messageAck">
	<html>
	<head>
	<style type="text/css">
	th
		{
			text-align:left;
			background-color:blue;
			color:white;
			font-family:Arial,Helvetica,sans-serif;
			
		}
	th.title
		{	font-size:150%;
		}
	th.subtitle
		{	background-color: #00CCCC;
		}
	th.subtitle1
		{	background-color: #46ADAD;
		}
	td
	{	
		color:blue;
		font-family:Arial,Helvetica,sans-serif;
		border-color:  Indigo ;
		border-width:  1 px ;
		
	}
	td.std
	{
		background-color:#EAF2FF;
		color:blue;
		font-family:Arial,Helvetica,sans-serif;
		border-color: Indigo  ;
		border-width:  1 px ;
			
			
	}
	tr.std
	{
		background-color:#EAF2D3;
		border: solid ;
		border-color: black;
		border-width: 1px 0;
	}
	
	tr.alt
	{  background-color: #99CCFF;
		border: solid ;
		border-color: black;
		border-width: 1px 0;
	}
</style>

	<title>Message Acknowledgment</title>
	</head>
	<body>
			<table>
				<tbody>
				<tr>
						<th class="title">Message Acknowledgment</th>
					</tr>
					<tr>
						<th class="subtitle1">Message Acknowledgment header</th>
					</tr>
					<tr>
						<td><xsl:apply-templates select="//header"/></td>
					</tr>
					<tr>
                        <th class="subtitle1">Message validation result</th>
                    </tr>
					<tr>
						<td><xsl:apply-templates select="//messageValRes"/></td>
					</tr>
					<tr>
                        <th class="subtitle1">PayloadAck</th>
                    </tr>
                    <tr>
                        <th class="subtitle">OperationAck</th>
                    </tr>
					<tr>
						<td><xsl:apply-templates select="//payloadAck"/></td>
					</tr>
					 <tr>
                        <th class="subtitle">Summary of Errors/Warnings</th>
                    </tr>
					<tr>
						<td><xsl:apply-templates select="//ackSumInfoList"/></td>
					</tr>
				</tbody>
			</table>
				
	</body>
</html>	
</xsl:template>
<!-- template for header-->
<xsl:template match="//header">
	<table width="100%" >
		<tr>
			<th>Message Type</th><td class="std" width="50%"><xsl:value-of select="normalize-space(type)"/></td>
		</tr>
		<tr>
			<th>Message version</th><td class="std" width="50%"><xsl:value-of select="normalize-space(version)"/></td>
		</tr>
		<tr>
			<th>Message indentifier from sender</th><td class="std" width="50%"><xsl:value-of select="normalize-space(senderMessageId)"/></td>
		</tr>
		<tr>
			<th>Sender organisation code</th><td class="std" width="50%"><xsl:value-of select="normalize-space(senderOrgCode)"/></td>
		</tr>
		<tr>
			<th>Receiver organisation code</th><td class="std" width="50%"><xsl:value-of select="normalize-space(receiverOrgCode)"/></td>
		</tr>
	</table>
</xsl:template>		

<!-- template for messageValRes -->
<xsl:template match="//messageValRes">
	<table width="100%" >
		<tr>
			<th>Message validation result code</th><td class="std" width="50%"><xsl:value-of select="normalize-space(messageValResCode)"/></td>
		</tr>
		<tr>
			<th>Reference to the detailed error list</th><td class="std" width="50%"><xsl:value-of select="normalize-space(detailedAckResId)"/></td>
		</tr>
	</table>
</xsl:template>		

<!-- template for payloadAck -->
<xsl:template match="//payloadAck">
	<xsl:apply-templates select="//operationAck"/>
	
</xsl:template>
	
<!-- template for operationAck for both messages-->
<xsl:template match="//operationAck">
	<table width="100%" >
		<tr>
			<th>Operation Type</th><td class="std" width="50%"><xsl:value-of select="normalize-space(opType)"/></td>
		</tr>
		<tr>
			<th>Dataset Id</th><td class="std" width="50%"><xsl:value-of select="normalize-space(datasetId)"/></td>
		</tr>
		<tr>
			<th>Data Collection Code</th><td class="std" width="50%"><xsl:value-of select="normalize-space(dcCode)"/></td>
		</tr>
		<tr>
			<th>Data Collection Table</th><td class="std" width="50%"><xsl:value-of select="normalize-space(dcTable)"/></td>
		</tr>
		<tr>
			<th>Sender's Code</th><td class="std" width="50%"><xsl:value-of select="normalize-space(orgCode)"/></td>
		</tr>
		<tr>
			<th>Operation Comment</th><td class="std" width="50%"><xsl:value-of select="normalize-space(opCom)"/></td>
		</tr>
		<tr>
			<th>Sent Date</th><td class="std" width="50%"><xsl:value-of select="normalize-space(opExecDate)"/></td>
		</tr>
		<tr>
			<th>Operation Result</th><td class="std" width="50%"><xsl:value-of select="normalize-space(opResCode)"/></td>
		</tr>
		<tr>
			<th>Operation result log</th><td class="std" width="50%"><xsl:value-of select="normalize-space(opResLog)"/></td>
		</tr>
		<tr>
			<th>Dataset Status</th><td class="std" width="50%"><xsl:value-of select="normalize-space(datasetStatus)"/></td>
		</tr>
	</table>
</xsl:template>		
	
<!-- template for ackSumInfoList-->
<xsl:template match="//ackSumInfoList">

<table width="100%">
	<tbody>
	
		<tr>
					<th>Type of validation information</th>
					<th>Error or warning feedback</th>
					<th>Example of checked record</th>
					<th>Value</th>
					<th>Number of Records</th>
		</tr>
		<xsl:apply-templates select="//ackSumInfo"/>
	</tbody>
</table>
	
</xsl:template>

<xsl:template match="//ackSumInfo">
<xsl:choose>
	<xsl:when test="count(preceding-sibling::ackSumInfo) mod 2">		
		<tr class="alt">
			<td><xsl:value-of select="normalize-space(infoType)" /></td>
			<td><xsl:value-of select="normalize-space(infoMessage)"/></td>
			<td><xsl:value-of select="normalize-space(./checkedDataElementExample/dataElement)"/></td>
			<td><xsl:value-of select="normalize-space(./checkedDataElementExample/value)"/></td>
			<td><xsl:value-of select="normalize-space(numRecords)"/></td>
		</tr>
		</xsl:when>
		<xsl:otherwise>
		<tr class="std">
			<td><xsl:value-of select="normalize-space(infoType)" /></td>
			<td><xsl:value-of select="normalize-space(infoMessage)"/></td>
			<td><xsl:value-of select="normalize-space(./checkedDataElementExample/dataElement)"/></td>
			<td><xsl:value-of select="normalize-space(./checkedDataElementExample/value)"/></td>
			<td><xsl:value-of select="normalize-space(numRecords)"/></td>
		</tr>
		</xsl:otherwise>
</xsl:choose>
</xsl:template>

<!-- template for Ackdetails-->
<xsl:template match="payloadAckDetails">
	<html>
	<head>
	<style type="text/css">
	table
		{
			collapse:collapse;
		}
	th
		{
			text-align:left;
			background-color:blue;
			color:white;
			font-family:Arial,Helvetica,sans-serif;
			
		}
	th.title
		{	
			font-size:150%;
		}
	th.subtitle
		{	
			background-color: #00CCCC;
		}
	td
		{	
			color:blue;
			font-family:Arial,Helvetica,sans-serif;
			border-bottom:1px solid black;
		}
	td.std
		{
			background-color:#EAF2FF;
			color:blue;
			font-family:Arial,Helvetica,sans-serif;
			border-bottom:1px solid black;
		}
	tr
		{	
			font-family:Arial,Helvetica,sans-serif;
			border-bottom:1px solid black;
			
		}
	tr.std
		{
			background-color:#EAF2D3;
			border-bottom:1pt solid black;
		}
	
	tr.alt
		{  
			background-color: #99CCFF;
			border-bottom:1pt solid black;
		}
</style>

<title>Result of detailed validation with Standard Sample Description Validation Rules</title>
	</head>
	<body>
			<table >
				<tbody>
					<tr>
						<th class="title">Result of detailed validation with Standard Sample Description Validation Rules</th>
					</tr>
					<tr>
						<th class="subtitle">Header</th>
					</tr>
					<tr>
						<td><xsl:apply-templates select="//operationAck"/></td>
					</tr>
					<tr>
                        <th class="subtitle">List of Errors</th>
                    </tr>
					<tr>
						<td><xsl:apply-templates select="./ackDetailsInfoList"/></td>
					</tr>
				</tbody>
			</table>
				
	</body>
</html>	
	
</xsl:template>
<xsl:template match="//ackDetailsInfoList">

<table width="100%">
	<tbody>
		<tr>
					<th>Business Rule Code</th>	
				    <th>Type</th>
					<th>Message</th>
					<th>Error sequence</th>
					<th>Context Description</th>
					
		</tr>
 <!-- added 7/12/2015	 -->
		
		<xsl:apply-templates select="./ackDetailsInfo"/>
	</tbody>
</table>
	
</xsl:template>

<xsl:template match="//ackDetailsInfo">

<tr>
				<td><xsl:value-of select="normalize-space(./businessRuleCode)"/></td>
				<td><xsl:value-of select="normalize-space(./infoType)"/></td>
				<td><xsl:value-of select="normalize-space(./infoMessage)"/></td>
				<td><xsl:value-of select="normalize-space(./errorSeq)"/></td>


<xsl:choose>
	<xsl:when test="./recordUniqueId">
		<td>
			<table>
				<tbody>
					<tr class="alt">
						<th>
							recordUniqueId
						</th>
						<xsl:for-each select="./checkedDataElement">
						<th>
							<xsl:value-of select="normalize-space(./dataElement)"/>
						</th>
						</xsl:for-each>
					</tr>
					
					<xsl:for-each select="./recordUniqueId">
						<xsl:variable name="record_position" select="position()"/>
						<tr>
							<td>
								<xsl:value-of select="current()"/> 
							</td>
							<xsl:for-each select="../checkedDataElement">
							<td>
								<xsl:value-of select="normalize-space(./value[$record_position])"/>
							</td>
							</xsl:for-each>	
						</tr>
					</xsl:for-each>
				</tbody>
			</table>
		</td>

	</xsl:when>


<xsl:otherwise>

		<td>
			<table>
				<tbody>
					<tr class="alt">
						<th>
							recordUniqueId
						</th>
						
						<th>
							<xsl:value-of select="normalize-space(./checkedDataElement/dataElement)"/>
						</th>
						
					</tr>
					
						<tr>
						<td>none</td>
						<td><xsl:value-of select="normalize-space(./checkedDataElement/value)"/></td>
					</tr>
				</tbody>
			</table>
		</td>
				
				

</xsl:otherwise>
</xsl:choose>
</tr>	





</xsl:template>
<!-- Added 30-03-2016-->


	
</xsl:stylesheet>
