package nl.carebridge.account.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Finds the visitor's IP address behind the platform's proxies. Every proxy adds the address it received
 * the request from to the end of X-Forwarded-For, so the last public address is the one Azure saw: anything
 * further left could have been written by the visitor. Inside a private network there is no visitor address.
 */
public final class ClientAddress {

	private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");
	private static final Pattern IPV6 = Pattern.compile("[0-9a-fA-F:]+");

	private ClientAddress() {
	}

	public static Optional<String> of(String forwardedFor, String remoteAddress) {
		String[] hops = forwardedFor == null ? new String[0] : forwardedFor.split(",");
		for (int i = hops.length - 1; i >= 0; i--) {
			Optional<String> address = publicAddress(hops[i].strip());
			if (address.isPresent()) {
				return address;
			}
		}
		return publicAddress(remoteAddress == null ? "" : remoteAddress.strip());
	}

	private static Optional<String> publicAddress(String value) {
		// Only literal addresses: a host name would make Java look it up in DNS.
		if (!IPV4.matcher(value).matches() && !(value.contains(":") && IPV6.matcher(value).matches())) {
			return Optional.empty();
		}
		try {
			InetAddress address = InetAddress.getByName(value);
			return isPrivate(address) ? Optional.empty() : Optional.of(address.getHostAddress());
		}
		catch (UnknownHostException ex) {
			return Optional.empty();
		}
	}

	private static boolean isPrivate(InetAddress address) {
		byte[] bytes = address.getAddress();
		boolean sharedAddressSpace = bytes.length == 4 && (bytes[0] & 0xff) == 100 && (bytes[1] & 0xc0) == 64;
		boolean uniqueLocalIpv6 = bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
		return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
				|| address.isAnyLocalAddress() || sharedAddressSpace || uniqueLocalIpv6;
	}
}
