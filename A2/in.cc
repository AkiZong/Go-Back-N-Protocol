#include<iostream>
using namespace std;

int main(){
	for(int k=0;k<32;k++){
		cout<<k<<endl;
		char a = 'a';
		for(int i=0;i<26;i++){
			for(int j=0;j<26;j++){
				cout<< char(a+(j+i)%26);
			}
			cout<<endl;
		}
		cout<<endl;
	}
}
