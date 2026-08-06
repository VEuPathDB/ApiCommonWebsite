# ApiCommonWebsite/Service

## Build chain

This module depends on three upstream builds. Run them in order before compiling here:

```bash
cd /home/maccallr/work/ai-wdk/project_home/install && mvn clean install
cd /home/maccallr/work/ai-wdk/project_home/WDK && mvn clean install -DskipTests
cd /home/maccallr/work/ai-wdk/project_home/ApiCommonWebsite/Model && mvn clean install -DskipTests
```

Then from this directory:

```bash
mvn compile        # quick check
mvn clean install -DskipTests  # full build
```
