#!/usr/bin/env ruby

KEEPASS_JAVA_URL = "https://github.com/jorabin/KeePassJava2.git"

PATCH1 = "/keepassjava2-patches/0001-replace-apache-codecs.patch"
PATCH2 = "/keepassjava2-patches/0002-protected-properties.patch"

SUCCESS = "success"
FAILURE = "failure"

def get_current_path()
    currentPath = `cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P`.strip

    if currentPath.empty?
        puts "Failed to locate project directory, try to 'cd' into project and run script again"
        exit 1
    end

    currentPath
end

def is_directory_exist(path)
    `[ -d #{path} ] && echo #{SUCCESS} || echo #{FAILURE}`.strip == SUCCESS
end

def is_file_exist(path)
    `[ -f #{path} ] && echo #{SUCCESS} || echo #{FAILURE}`.strip == SUCCESS
end

def clone_keepass_java_project()
    path = get_current_path() + "/tmp/KeePassJava2"
    isAlreadyCloned = is_directory_exist(path)

    if isAlreadyCloned == false
        `git clone #{KEEPASS_JAVA_URL} "#{path}"`
    end

    path
end

def copy_sources(srcPath, dstPath)
    puts "copy sources from: #{srcPath} to #{dstPath}"
    `[ -d #{dstPath} ] && rm -r "#{dstPath}"`
    `cp -r "#{srcPath}" "#{dstPath}"`
end

def apply_patch(patchFilePath)
    puts "Applying patch: #{patchFilePath}"
    `cd "#{get_current_path()}"`

    isApplied = `git apply "#{patchFilePath}" && echo "#{SUCCESS}"`.strip == SUCCESS
    if isApplied
        puts "Successfully applied"
    else
        puts "Failed to apply path file: #{patchFilePath}"
        exit 1
    end
end

def build_library(libraryModuleName, flavor, libraryFilePath)
    command = "./gradlew :libs:#{libraryModuleName}:assemble#{flavor}"

    puts "Assembling library: #{libraryModuleName}"
    puts "   executed command: #{command}"
    
    `cd "#{get_current_path()}"`
    `[ -f #{libraryFilePath} ] && rm -r "#{libraryFilePath}"`
    isSuccessfully = `#{command} > /dev/null 2>&1 && echo #{SUCCESS} || echo #{FAILURE}`.strip == SUCCESS
    if isSuccessfully
        puts "#{libraryModuleName} is assembled successfully"
    else
        puts "Failed to assemble module: #{libraryModuleName}"
        exit 1
    end
end

def main()
    currentPath = get_current_path()

    # clone KeePassJava2 project from github
    keepassJavaPath = clone_keepass_java_project()

    # copy sources to the libs/* 
    copy_sources("#{keepassJavaPath}/database/src/main/java/", "#{currentPath}/libs/KeePassJava2-database/src/main/java")
    copy_sources("#{keepassJavaPath}/simple/src/main/java/", "#{currentPath}/libs/KeePassJava2-simple/src/main/java")
    copy_sources("#{keepassJavaPath}/kdbx/src/main/java/", "#{currentPath}/libs/KeePassJava2-kdbx/src/main/java")

    # apply pathces
    apply_patch("#{currentPath + PATCH1}")
    apply_patch("#{currentPath + PATCH2}")

    # build Release libraries
    databaseAarRelease = "#{currentPath}/libs/KeePassJava2-database/build/outputs/aar/KeePassJava2-database-release.aar"
    simpleAarRelease = "#{currentPath}/libs/KeePassJava2-simple/build/outputs/aar/KeePassJava2-simple-release.aar"
    kdbxAarRelease = "#{currentPath}/libs/KeePassJava2-kdbx/build/outputs/aar/KeePassJava2-kdbx-release.aar"
    build_library("KeePassJava2-database", "Release", databaseAarRelease)
    build_library("KeePassJava2-simple", "Release", simpleAarRelease)
    build_library("KeePassJava2-kdbx", "Release", kdbxAarRelease)

    # build Debug libraries
    databaseAarDebug = "#{currentPath}/libs/KeePassJava2-database/build/outputs/aar/KeePassJava2-database-debug.aar"
    simpleAarDebug = "#{currentPath}/libs/KeePassJava2-simple/build/outputs/aar/KeePassJava2-simple-debug.aar"
    kdbxAarDebug = "#{currentPath}/libs/KeePassJava2-kdbx/build/outputs/aar/KeePassJava2-kdbx-debug.aar"
    build_library("KeePassJava2-database", "Debug", databaseAarDebug)
    build_library("KeePassJava2-simple", "Debug", simpleAarDebug)
    build_library("KeePassJava2-kdbx", "Debug", kdbxAarDebug)

    # copy assembled libraries to main project 
    libsPath = "#{currentPath}/app/libs"
    `cp -rf "#{databaseAarRelease}" "#{libsPath}"`
    `cp -rf "#{databaseAarDebug}" "#{libsPath}"`

    `cp -rf "#{simpleAarRelease}" "#{libsPath}"`
    `cp -rf "#{simpleAarDebug}" "#{libsPath}"`

    `cp -rf "#{kdbxAarRelease}" "#{libsPath}"`
    `cp -rf "#{kdbxAarDebug}" "#{libsPath}"`

    puts "SUCCESS"
end

main()