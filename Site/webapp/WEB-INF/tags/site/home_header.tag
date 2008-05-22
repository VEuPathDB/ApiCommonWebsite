<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>

<%@ attribute name="refer" 
 			  type="java.lang.String"
			  required="true" 
			  description="Page calling this tag"
%>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>CryptoDB -- Cryptosporidium Genome Resources</title>
<link href="/assets/css/crypto.css" rel="stylesheet" type="text/css" />
<c:if test="${refer == 'home'}">
<style type="text/css">
<!--
body {
	background-image: url(/assets/images/crypto/background.jpg);
	background-repeat: repeat-x;
    }
#header {
		height: 159px;
		background-image: url(/assets/images/crypto/backgroundtop.jpg);
	}

-->
</style>
</c:if>
<c:if test="${refer != 'home'}">
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
-->
</style>
</c:if>

<site:jscript />
</head>

<body>


<div id="header">
  <c:if test="${refer == 'home'}">
  	<p><img src="/assets/images/crypto/title1.png" alt="CryptoDB" width="398" height="87" align="left" /></p>
  </c:if>
  <c:if test="${refer != 'home'}">
	<p><a href="http://www.cryptodb.org"><img src="/assets/images/crypto/title_s.png" alt="CryptoDB" width="256" height="56" align="left" /></a></ p>
  </c:if>
  <p>&nbsp;</p>
  <p>Version 3.8<br />
    March 15, 2008</p>
</div>
