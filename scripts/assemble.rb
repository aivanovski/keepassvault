#!/usr/bin/env ruby

DEBUG_GPLAY_APK_PATH = 'app/build/outputs/apk/gplay/debug/app-gplay-debug.apk'.freeze
RELEASE_GPLAY_APK_PATH = 'app/build/outputs/apk/gplay/release/app-gplay-release.apk'.freeze
RELEASE_GPLAY_AAB_PATH = 'app/build/outputs/bundle/gplayRelease/app-gplay-release.aab'.freeze

DEBUG_FDROID_APK_PATH = 'app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk'.freeze
RELEASE_FDROID_APK_PATH = 'app/build/outputs/apk/fdroid/release/app-fdroid-release.apk'.freeze

def project_directory?
  `pwd | awk -F'/' '{print $NF}'`.strip == 'kpassnotes'
end

def dir_path
  `pwd`.strip
end

def check_conditions
  if ARGV.empty?
    puts 'Please specify output path'
    exit 1
  end

  unless project_directory?
    puts 'Script should be launched from project root'
    exit 1
  end
end

def remove_files(files)
  files.each do |file|
    if File.exist? file
      puts "Removing file: #{file}"
      `rm #{file}`
    end
  end
end

def copy_files(sources, destinations)
  for i in 0..(sources.size - 1) do
    puts "Copying file to: #{destinations[i]}"
    `cp "#{sources[i]}" "#{destinations[i]}"`
  end
end

def exists?(files)
  files.all? { |file| File.exist? file }
end

def get_app_version
  major = `cat app/build.gradle | grep 'def versionMajor' | cut -d= -f2`.strip
  minor = `cat app/build.gradle | grep 'def versionMinor' | cut -d= -f2`.strip
  patch = `cat app/build.gradle | grep 'def versionPatch' | cut -d= -f2`.strip
  "#{major}.#{minor}.#{patch}"
end

def assemble
  puts 'Assembling Gplay Debug...'
  `./gradlew app:assembleGplayDebug`

  puts 'Assembling Gplay Release...'
  `./gradlew app:assembleGplayRelease`

  puts 'Assembling Bundle Gplay Release...'
  `./gradlew app:bundleGplayRelease`

  puts 'Assembling FDroid Debug...'
  `./gradlew app:assembleFdroidDebug`

  puts 'Assembling FDroid Release...'
    `./gradlew app:assembleFdroidRelease`
end

def main
  check_conditions

  files = [
    "#{dir_path}/#{DEBUG_GPLAY_APK_PATH}",
    "#{dir_path}/#{RELEASE_GPLAY_APK_PATH}",
    "#{dir_path}/#{RELEASE_GPLAY_AAB_PATH}",
    "#{dir_path}/#{DEBUG_FDROID_APK_PATH}",
    "#{dir_path}/#{RELEASE_FDROID_APK_PATH}",
  ]

  remove_files(files)
  assemble

  if exists?(files)
    puts 'Success'
    output_dir = ARGV[0].strip.gsub('~', '$HOME')
    version = get_app_version
    destinations = [
      "#{output_dir}/kpassnotes-gplay-debug-#{version}.apk",
      "#{output_dir}/kpassnotes-gplay-release-#{version}.apk",
      "#{output_dir}/kpassnotes-gplay-release-#{version}.aab",
      "#{output_dir}/kpassnotes-fdroid-debug-#{version}.apk",
      "#{output_dir}/kpassnotes-fdroid-release-#{version}.apk",
    ]
    copy_files(files, destinations)
  else
    puts 'Failed'
  end
end

main
