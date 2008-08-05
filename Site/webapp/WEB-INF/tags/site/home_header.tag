<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>

<%@ attribute name="refer" 
 			  type="java.lang.String"
			  required="true" 
			  description="Page calling this tag"
%>

<c:set var="siteName" value="${applicationScope.wdkModel.name}" />

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>CryptoDB -- Cryptosporidium Genome Resources</title>
<link href="/assets/css/crypto.css" rel="stylesheet" type="text/css" />
<link rel="stylesheet" href="/assets/css/history.css" type="text/css"/>
<link rel="stylesheet" type="text/css" href="/assets/css/Strategy.css" />
<link rel="StyleSheet" href="/assets/css/filter_menu.css" type="text/css"/>

<style type="text/css">
<!--
body {
	background-image: url(/assets/images/crypto/background_s.jpg);
	background-repeat: repeat-x;
    }
#header {
	height: 104px;
	background-image: url(/assets/images/crypto/backgroundtop_s.jpg);
}
#header p {
	font-size: 9px;
}
-->
</style>

<site:jscript />
</head>

<body>

<div id="header2">
   <div id="header_rt"><a href="http://eupathdb.org"><img src="../assets/images/partofeupath.png" alt="EuPathDB Homepage" 
	width="148" height="23" /></a>
       <div id="bottom">
	  <site:quickSearch /><br />
	  <a href="#">About ${siteName}</a> | <a href="#">Help</a> | <a href="#">Contact Us</a> | <a href="#">Log In/Register</a>
	  <%-- possible style when a user is login....
       	  <a href="#">About ${siteName}</a> | <a href="#">Help</a> | <a href="#">Contact Us</a> | <a href="#">Logout</a>
 	  <br /><b style='color:black'>John Doe</b> | <a href="#">Profile</a>
	  --%>
       </div>
   </div>

   <p><a href="/"><img src="../assets/images/crypto/title_s.png" alt="CryptoDB" 
	width="318" height="64" align="left" /></a></p>
   <p>&nbsp;</p>
   <p>Version 3.8<br />
   October 15, 2008</p>
</div> 
