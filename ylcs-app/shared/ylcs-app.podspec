Pod::Spec.new do |spec|
    spec.name                     = 'ylcs-app'
    spec.version                  = '3.3.1'
    spec.homepage                 = 'https://github.com/rachel-ylcs/ylcs-kmp'
    spec.source                   = { :http=> ''}
    spec.authors                  = ''
    spec.license                  = ''
    spec.summary                  = '银临茶舍KMP跨平台APP'
    spec.vendored_frameworks      = 'build/cocoapods/framework/ylcs_app.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target    = '16.0'
    if !Dir.exist?('build/cocoapods/framework/ylcs_app.framework') || Dir.empty?('build/cocoapods/framework/ylcs_app.framework')
        raise "
        Kotlin framework 'ylcs_app' doesn't exist yet, so a proper Xcode project can't be generated.
        'pod install' should be executed after running ':generateDummyFramework' Gradle task:
            ./gradlew :ylcs-app:shared:generateDummyFramework
        Alternatively, proper pod installation is performed during Gradle sync in the IDE (if Podfile location is set)"
    end
    spec.xcconfig = {
        'ENABLE_USER_SCRIPT_SANDBOXING' => 'NO',
    }
    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':ylcs-app:shared',
        'PRODUCT_MODULE_NAME' => 'ylcs_app',
    }
    spec.script_phases = [
        {
            :name => 'Build ylcs-app',
            :execution_position => :before_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                    echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                    exit 0
                fi
                set -ev
                REPO_ROOT="$PODS_TARGET_SRCROOT"
                "$REPO_ROOT/../../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:syncFramework \
                    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
                    -Pkotlin.native.cocoapods.archs="$ARCHS" \
                    -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
            SCRIPT
        }
    ]
    spec.resources = ['build\compose\cocoapods\compose-resources']
end
