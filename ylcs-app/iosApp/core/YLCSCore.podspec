Pod::Spec.new do |spec|
  spec.name         = "YLCSCore"
  spec.version      = "0.0.1"
  spec.summary      = "银临茶舍 Core Framework"
  spec.homepage     = "https://github.com/rachel-ylcs/ylcs-kmp"
  spec.license      = ""
  spec.authors      = ""
  spec.swift_version = "5.0"
  spec.ios.deployment_target = "16.0"
  spec.source        = { :http=> "" }
  spec.source_files  = "*.swift", "**/*.{h,m}"
  spec.exclude_files = "Exclude"

  spec.pod_target_xcconfig = {
    'PRODUCT_MODULE_NAME' => "YLCSCore",
  }
end