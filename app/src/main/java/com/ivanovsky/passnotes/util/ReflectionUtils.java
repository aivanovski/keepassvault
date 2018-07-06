package com.ivanovsky.passnotes.util;

import com.annimon.stream.Stream;

public class ReflectionUtils {

	public static boolean containsInterfaceInClass(Class classType, Class interfaceType) {
		return Stream.of(classType.getInterfaces())
				.anyMatch(classInterface -> classInterface == interfaceType);
	}
}

