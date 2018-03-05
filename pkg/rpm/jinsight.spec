%global reporoot       %{_topdir}/../../..

BuildArch:      @RPM_TARGET@
Name:           jinsight
Group:          System/Monitoring
Version:        @PACKAGE_VERSION@
Release:        @PACKAGE_REVISION@
License:        Apache License
Summary:        Apptuit JInsight - A Java agent to collect metrics from Java applications
URL:            https://apptuit.ai/jinsight
Requires:       xcollector
Packager:       JInsight Maintainers <hello+jinsight@apptuit.ai>


%description
Apptuit JInsight is a java agent that instruments byte code in a JVM
to transparently collect performance metrics about various sub-systems
in your application.

%install
mkdir -p %{buildroot}/usr/share/java/
mkdir -p %{buildroot}/etc/jinsight/
chmod -R 755 %{buildroot}/*

%{__install} -m 644 %{reporoot}/target/jinsight-@JAR_VERSION@.jar %{buildroot}/usr/share/java/
ln -rs %{buildroot}/usr/share/java/jinsight-@JAR_VERSION@.jar %{buildroot}/usr/share/java/jinsight.jar
%{__install} -m 644 %{reporoot}/pkg/common/jinsight-config.properties %{buildroot}/etc/jinsight


%files
/usr/share/java/
/etc/jinsight  %config(noreplace)
